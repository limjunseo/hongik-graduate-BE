# POFIT QA: cheapest EC2 deployment

Scope: **one** Ubuntu 24.04 ARM64 EC2 `t4g.micro` (1 GiB RAM), 8 GiB encrypted gp3, file-backed H2, 1 GiB swap, Java 17, Caddy HTTPS. No RDS, ELB, NAT gateway, Elastic IP or managed DNS. This is QA only, not production-grade.

## AWS provisioning (Windows PowerShell)

1. Sign in with `aws login --profile pofit-qa`. **Verify which AWS account is selected.** Do not overwrite an unrelated CLI profile. Use an IAM role with least privileges where possible.
2. In the repository root run `aws --profile pofit-qa sts get-caller-identity`.
3. Invoke `./deploy/qa/provision.ps1 -AwsProfile pofit-qa -ConfirmAccountId <YOUR_12_DIGIT_AWS_ACCOUNT_ID>`. It will refuse mismatched accounts, duplicate instance tags, existing keys, or no default public subnet.
4. Script prints `PUBLIC_IP`, `SSH_KEY`, and `API_URL`. Wait until cloud-init finishes before uploading a JAR.
5. Run `./deploy/qa/deploy-jar.ps1 -PublicIp <PUBLIC_IP> -KeyPath <SSH_KEY>` (requires JDK 17+ and Maven Wrapper; builds locally, not on the 1 GiB VM).
6. Check `https://<DASHED_IP>.sslip.io/v3/api-docs`. Use that full origin as `QA_BACKEND_ORIGIN` in the Next.js Vercel environment; redeploy the frontend after changing it.

The backend API listens on localhost:8080 only. Security group opens 22 **only to the current public IP**, and 80/443 for Caddy. H2 console is disabled. Swagger/OpenAPI documentation remains exposed to the internet, so use disposable QA data only.

The hostname uses the community DNS service sslip.io and Let's Encrypt via Caddy. It requires no account, but is a convenience service and not suitable for mission-critical deployments. If the instance stops/starts, its public IPv4 may change: `pofit-refresh-domain` updates Caddy but you must update the frontend `QA_BACKEND_ORIGIN` and redeploy. An Elastic IP would avoid that at the cost of IP charges even while stopped.

## Operations

- Logs: `sudo journalctl -u pofit-qa -f`
- First-boot logs: `sudo tail -100 /var/log/cloud-init-output.log`
- Domain: `cat /opt/pofit-qa/public-host.txt`
- Restart: `sudo systemctl restart pofit-qa`
- DB: `/opt/pofit-qa/data/hongik-graduate.mv.db`
- Uploads: `/opt/pofit-qa/uploads/imports/`
- AI is disabled by default (`AI_ENABLED=false` and `AI_PROVIDER=mock`). Email verification requires real SMTP credentials.
- Back up `data/` and `uploads/` **after stopping the Java service**, and copy the backup off the instance. The 8 GiB root EBS is deleted on instance termination.
- Configure AWS Budgets alerts and stop the instance when QA is idle. Stopped instances still incur EBS charges. Watch T4g CPU credit billing (this script uses Standard mode).
- QA sizing is not guaranteed: monitor `free -h`, `df -h`, and service logs. If the JVM fails to start or OOMs, switch to 2 GiB t4g.small.

**Nothing in these scripts runs against AWS unless you explicitly invoke `provision.ps1` with the confirmed account ID.**
