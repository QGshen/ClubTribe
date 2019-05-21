package clubtribe.dao;

import clubtribe.pojo.ClubMember;

public interface ClubMemberMapper {

    /**
     * 插入新成员
     *
     * @param member
     */
    int insert(ClubMember member);

    /**
     * 签到
     *
     * @param clubMember
     * @return
     */
    int sign(ClubMember clubMember);

    /**
     * 获取签到时间
     *
     * @param clubMember
     * @return
     */
    String getsigntime(ClubMember clubMember);

    /**
     * 签到天数加一
     */
    void UPdatemsign(ClubMember clubMember);

    /**
     * 每天清零签到
     */
    void updataED();

    /**
     * 每月清零签到
     */
    void updataEM();

}
