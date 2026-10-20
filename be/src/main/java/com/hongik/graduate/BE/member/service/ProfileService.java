package com.hongik.graduate.BE.member.service;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.dto.ProfileResponse;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final MemberRepository members;
    public ProfileService(MemberRepository members) { this.members = members; }
    @Transactional(readOnly = true)
    public ProfileResponse get(UUID id) { return ProfileResponse.from(find(id)); }
    @Transactional
    public ProfileResponse basic(UUID id, Integer age, String gender, String school) {
        if (age == null || age < 1 || age > 120 || invalid(gender,30) || invalid(school,150))
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Member m = find(id);
        m.updateBasic(age,gender.trim(),school.trim());
        return ProfileResponse.from(m);
    }
    @Transactional
    public ProfileResponse job(UUID id, String desiredJob) {
        if (invalid(desiredJob,100)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Member m = find(id);
        if (!m.isBasicCompleted()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        m.updateJob(desiredJob.trim());
        return ProfileResponse.from(m);
    }
    private boolean invalid(String s,int max) { return s==null||s.isBlank()||s.length()>max; }
    private Member find(UUID id) {
        return members.findById(id).orElseThrow(()->new BusinessException(ErrorCode.INVALID_REQUEST));
    }
}
