package com.bom.mapper;

import com.bom.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    List<SysUser> findPendingUsers();

    int insert(SysUser user);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("rejectReason") String rejectReason, @Param("reviewedBy") Long reviewedBy);

    List<SysUser> findAll();

    List<SysUser> findApprovedUsers();

    int update(SysUser user);
}
