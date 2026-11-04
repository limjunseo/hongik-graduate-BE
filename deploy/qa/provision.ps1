param(
  [Parameter(Mandatory = $true)][ValidatePattern("^\d{12}$")][string]$ConfirmAccountId,
  [string]$AwsProfile = "default",
  [string]$Region = "ap-northeast-2"
)
$ErrorActionPreference = "Stop"

$who = & aws --profile $AwsProfile --region $Region sts get-caller-identity --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or !$who) { throw "AWS CLI login required: aws login --profile $AwsProfile" }
if ($who.Account -ne $ConfirmAccountId) {
  throw "Account mismatch! Expected $ConfirmAccountId, got $($who.Account). Nothing created."
}

$existing = & aws --profile $AwsProfile --region $Region ec2 describe-instances --filters "Name=tag:Name,Values=pofit-qa-be" "Name=instance-state-name,Values=pending,running,stopping,stopped" --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Cannot check existing instances" }
if (@($existing.Reservations | ForEach-Object { $_.Instances }).Count -gt 0) {
  throw "pofit-qa-be already exists. Refusing to create a duplicate."
}

$vpclist = & aws --profile $AwsProfile --region $Region ec2 describe-vpcs --filters "Name=isDefault,Values=true" --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or !@($vpclist.Vpcs).Count) { throw "Default VPC not found" }
$vpc = @($vpclist.Vpcs)[0].VpcId

$subnets = & aws --profile $AwsProfile --region $Region ec2 describe-subnets --filters ("Name=vpc-id,Values=" + $vpc) --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Subnet lookup failed" }
$subnet = @($subnets.Subnets | Where-Object { $_.MapPublicIpOnLaunch } | Select-Object -First 1)
if (!$subnet.Count) { throw "No public default subnet available" }
$subnetId = $subnet[0].SubnetId

$ami = & aws --profile $AwsProfile --region $Region ssm get-parameter --name "/aws/service/canonical/ubuntu/server/24.04/stable/current/arm64/hvm/ebs-gp3/ami-id" --query "Parameter.Value" --output text
if ($LASTEXITCODE -ne 0 -or !$ami) { throw "Ubuntu ARM64 AMI lookup failed" }
$rootDevice = & aws --profile $AwsProfile --region $Region ec2 describe-images --image-ids $ami --query "Images[0].RootDeviceName" --output text
if ($LASTEXITCODE -ne 0 -or !$rootDevice) { throw "Root device lookup failed" }

$myIp = (Invoke-RestMethod -Uri "https://checkip.amazonaws.com/" -TimeoutSec 10).Trim()
if ($myIp -notmatch "^\d{1,3}(\.\d{1,3}){3}$") { throw "Cannot determine local IPv4" }

$keyName = "pofit-qa-" + $ConfirmAccountId + "-" + $Region
$keyDir = Join-Path $env:USERPROFILE ".ssh"
$keyPath = Join-Path $keyDir ($keyName + ".pem")
if (Test-Path $keyPath) { throw "PEM exists: $keyPath (refusing overwrite)" }
$existingKey = & aws --profile $AwsProfile --region $Region ec2 describe-key-pairs --filters ("Name=key-name,Values=" + $keyName) --query "KeyPairs[].KeyName" --output text
if ($LASTEXITCODE -ne 0 -or $existingKey) { throw "Key pair already exists or lookup failed" }

Write-Host "Creating billable t4g.micro, 8GiB gp3 in $ConfirmAccountId / $Region."
Write-Host "SSH limited to $myIp/32. HTTPS 80/443 public; port 8080 private."

$sg = & aws --profile $AwsProfile --region $Region ec2 create-security-group --group-name ($keyName + "-sg") --description "POFIT QA HTTPS SSH" --vpc-id $vpc --query GroupId --output text
if ($LASTEXITCODE -ne 0 -or !$sg) { throw "Security group creation failed" }
foreach ($rule in @(@(22, ($myIp + "/32")), @(80, "0.0.0.0/0"), @(443, "0.0.0.0/0"))) {
  & aws --profile $AwsProfile --region $Region ec2 authorize-security-group-ingress --group-id $sg --protocol tcp --port $rule[0] --cidr $rule[1] | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Ingress failed, clean up security group: $sg" }
}

$material = & aws --profile $AwsProfile --region $Region ec2 create-key-pair --key-name $keyName --query KeyMaterial --output text
if ($LASTEXITCODE -ne 0 -or !$material) { throw "Key pair failed, clean up security group: $sg" }
New-Item -ItemType Directory -Force -Path $keyDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$lf = [char]10
[System.IO.File]::WriteAllText($keyPath, (($material -join $lf).Trim() + $lf), $utf8)
& icacls $keyPath /inheritance:r /grant:r ($env:USERNAME + ":(R)") | Out-Null

$userdata = Join-Path $PSScriptRoot "cloud-init.sh"
if (!(Test-Path $userdata)) { throw "Missing cloud-init.sh" }
$mapping = "DeviceName=" + $rootDevice + ",Ebs={VolumeSize=8,VolumeType=gp3,Encrypted=true,DeleteOnTermination=true}"
$instance = & aws --profile $AwsProfile --region $Region ec2 run-instances --image-id $ami --instance-type t4g.micro --subnet-id $subnetId --security-group-ids $sg --key-name $keyName --block-device-mappings $mapping --metadata-options "HttpTokens=required,HttpEndpoint=enabled" --credit-specification "CpuCredits=standard" --user-data ("file://" + $userdata) --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=pofit-qa-be}]" --query "Instances[0].InstanceId" --output text
if ($LASTEXITCODE -ne 0 -or !$instance) { throw "Create instance failed; inspect SG=$sg KEY=$keyName" }

& aws --profile $AwsProfile --region $Region ec2 wait instance-running --instance-ids $instance
$publicIp = & aws --profile $AwsProfile --region $Region ec2 describe-instances --instance-ids $instance --query "Reservations[0].Instances[0].PublicIpAddress" --output text
if ($LASTEXITCODE -ne 0 -or !$publicIp) { throw "Created $instance, public IP lookup failed" }

$apiHost = ($publicIp -replace "\.", "-") + ".sslip.io"
Write-Host "INSTANCE_ID=$instance"
Write-Host "PUBLIC_IP=$publicIp"
Write-Host "SSH_KEY=$keyPath"
Write-Host "API_URL=https://$apiHost"
Write-Host ('Next: .\deploy\qa\deploy-jar.ps1 -PublicIp ' + $publicIp + ' -KeyPath "' + $keyPath + '"')
