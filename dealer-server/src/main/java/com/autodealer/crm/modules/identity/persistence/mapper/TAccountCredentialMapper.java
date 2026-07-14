package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.application.api.enums.CredentialPurpose;
import com.autodealer.crm.modules.identity.persistence.model.TAccountCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TAccountCredentialMapper {
    int insert(TAccountCredential credential);
    TAccountCredential selectByDigest(String tokenDigest);
    TAccountCredential selectActiveByUserAndPurpose(@Param("userId") Integer userId,
                                                     @Param("purpose") CredentialPurpose purpose);
    TAccountCredential selectLatestByUserAndPurposesForUpdate(@Param("userId") Integer userId,
                                                               @Param("purposes") List<CredentialPurpose> purposes);
    int revokeActive(@Param("userId") Integer userId,
                     @Param("purpose") CredentialPurpose purpose,
                     @Param("revokedAt") LocalDateTime revokedAt);
    int revokeAllActive(@Param("userId") Integer userId,
                        @Param("revokedAt") LocalDateTime revokedAt);
    int consumeByIdAndVersion(@Param("id") Long id,
                              @Param("expectedVersion") Integer expectedVersion,
                              @Param("consumedAt") LocalDateTime consumedAt);
    TAccountCredential selectById(Long id);
    int bindTokenDigest(@Param("id") Long id,
                        @Param("expectedCommitment") String expectedCommitment,
                        @Param("tokenDigest") String tokenDigest,
                        @Param("now") LocalDateTime now);
    int revokeIssuedById(@Param("id") Long id,@Param("revokedAt") LocalDateTime revokedAt);
}
