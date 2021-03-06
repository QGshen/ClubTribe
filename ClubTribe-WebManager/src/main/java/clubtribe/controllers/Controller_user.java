package clubtribe.controllers;

import clubtribe.pojo.Activity;
import clubtribe.pojo.Club;
import clubtribe.pojo.ClubMember;
import clubtribe.services.ActivityServices;
import clubtribe.services.ClubMemberServices;
import clubtribe.services.ClubServices;
import clubtribe.services.UserServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import commom.Generator;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("user")
public class Controller_user {

    @Autowired
    private ClubServices clubsServices;
    @Autowired
    private UserServices userServices;
    @Autowired
    private ClubMemberServices clubMemberServices;
    @Autowired
    private ActivityServices activityServices;

    /**
     * 登录进入社团主页
     *
     * @param clubid
     * @return
     */
    @RequestMapping("clubhome")
    public ModelAndView inter(String clubid) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("clubhome");
        modelAndView.addObject("clubid", clubid);
        return modelAndView;
    }

    /**
     * 根据id获取name init
     *
     * @param userid
     * @param clubid
     * @return
     */
    @RequestMapping(value = "init", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String getinfo(String userid, String clubid) {
        String username = null;
        String clubname = null;
        if (!"".equals(userid)) {
            username = userServices.findnamebyid(Integer.parseInt(userid));
        }
        if (!"".equals(clubid)) {
            clubname = clubsServices.findnamebyid(Integer.parseInt(clubid));
        }
        return username + "@" + clubname;
    }

    /**
     * 加入社团申请
     *
     * @param userid
     * @param clubid
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping(value = "joinapply", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String join(String userid, String clubid) throws IOException, ClassNotFoundException {
        String str = "加入失败";
        String[] clubids = new String[0];
        if (userServices.getuserclubs(Integer.parseInt(userid)) != null) {
            clubids = userServices.getuserclubs(Integer.parseInt(userid)).split("@");
        }
        //规定最多五个clubid7
        if (clubids.length >= 5) {
            str = "加入失败 每个用户最多加入5个社团";
        } else {
            if (Arrays.asList(clubids).contains(clubid)) {
                str = "你已是该社团成员 无需再次擦操作！";
            } else {
                if (clubsServices.getmsg(Integer.parseInt(clubid)) == null || clubsServices.getmsg(Integer.parseInt(clubid)).length() == 0) {
                    String filename = "D:/clubtribefile/clubmsg/msg" + clubid + ".txt";
                    File file = new File(filename);
                    if (!file.exists()) {
                        file.createNewFile();
                        Club club = new Club();
                        club.setClubid(clubid);
                        club.setMsg(file.toString());
                        clubsServices.initmsg(club);
                        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
                        ArrayList<String> list = new ArrayList<>();
                        oos.writeObject(list);
                        oos.close();
                    }
                }
                String filepath = clubsServices.getmsg(Integer.parseInt(clubid));
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
                ArrayList<String> list = (ArrayList<String>) ois.readObject();
                ois.close();
                if (list.contains(userid)) {
                    str = "你已发送过加入申请 不能重复申请 亲耐心等待审核";
                } else {
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
                    list.add(userid);
                    oos.writeObject(list);
                    oos.close();
                    str = "加入申请已发送!";
                }
            }
        }
        return str;
    }

    /**
     * 判断是否为管理员
     *
     * @param userid
     * @param clubid
     * @return
     */
    @RequestMapping("ifadmin")
    @ResponseBody
    public String ifadmin(String userid, String clubid) {
        String[] admin = clubsServices.getadmin(Integer.parseInt(clubid)).split("@");
        if (Arrays.asList(admin).contains(userid) && !userid.equals("") && userid != null) {
            return "true";
        }
        return "false";
    }

    /**
     * 判断是否为社团成员
     *
     * @param userid
     * @param clubid
     * @return
     */
    @RequestMapping("ifmember")
    @ResponseBody
    public String ifmember(String userid, String clubid) {
        if (userid != null && userid.length() != 0) {
            String clubids = userServices.getuserclubs(Integer.parseInt(userid));
            String[] clubs = clubids.split("@");
            if (Arrays.asList(clubs).contains(clubid)) {
                return "true";
            }
        }
        return "false";
    }

    /**
     * 进入管理员页面
     *
     * @param userid
     * @param clubid
     * @return
     * @throws IOException
     */
    @RequestMapping("interadmin")
    public ModelAndView interadmin(String userid, String clubid) throws IOException {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("userid", userid);
        modelAndView.addObject("clubid", clubid);
        modelAndView.setViewName("clubadmin");
        return modelAndView;
    }

    /**
     * 社团成员签到
     *
     * @param userid
     * @param clubid
     * @return
     */
    @RequestMapping(value = "sign", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String sign(String userid, String clubid) {
        String flag = "签到失败 仅限社员";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] clubids = userServices.getuserclubs(Integer.parseInt(userid)).split("@");
        if (Arrays.asList(clubids).contains(clubid)) {
            ClubMember clubMember = new ClubMember();
            clubMember.setClubid(clubid);
            clubMember.setUserid(userid);
            String signtime = clubMemberServices.getsigntime(clubMember);
            if (signtime.length() == 0 || signtime == null) {
                String time = df.format(new Date());
                clubMember.setSign(time);
                if (clubMemberServices.sign(clubMember) != 0) {
                    flag = "签到成功";
                }
            } else {
                flag = "你今日已经签到过";
            }
        }
        return flag;
    }

    /**
     * 获取签到信息
     */
    @RequestMapping(value = "getsignmsg")
    @ResponseBody
    public String getsignmsg(String clubid) throws ParseException, JsonProcessingException {
        ClubMember[] list = clubMemberServices.getmembermsg(clubid);
        ArrayList<ClubMember> newlist = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            if (list[i].getSign() != "" && list[i].getSign().length() != 0) {
                newlist.add(list[i]);
            }
        }
        ClubMember[] newlist1 = newlist.toArray(new ClubMember[newlist.size()]);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (newlist1.length >= 2) {
            ClubMember tep = new ClubMember();
            for (int i = 0; i < newlist1.length; i++) {
                for (int j = i + 1; j < list.length; j++) {
                    Date date1 = sdf.parse(newlist1[i].getSign());
                    Date date2 = sdf.parse(newlist1[j].getSign());
                    if (date1.compareTo(date2) > 0) {
                        tep = newlist1[i];
                        newlist1[i] = newlist1[j];
                        newlist1[j] = tep;
                    }
                }
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(newlist);
    }

    /**
     * 获取月签到信息
     */
    @RequestMapping(value = "getmsign")
    @ResponseBody
    public String getmsign(String clubid) throws JsonProcessingException {
        ClubMember[] list = clubMemberServices.getmembermsg(clubid);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(list);
    }

    /**
     * 留言墙
     *
     * @param clubid
     * @param msg
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping(value = "msgboard")
    @ResponseBody
    public String msgboard(String clubid, String msg) throws IOException, ClassNotFoundException {
        String filepath = clubsServices.getmsgboard(Integer.parseInt(clubid));
        ArrayList<String> list = null;
        if (filepath == null || filepath.length() == 0) {
            filepath = "D:/clubtribefile/clubmsg/msgboard" + clubid + ".txt";
            File file = new File(filepath);
            if (!file.exists()) {
                file.createNewFile();
                Club club = new Club();
                club.setMsgboard(file.toString());
                club.setClubid(clubid);
                clubsServices.initmsgboard(club);
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
                list = new ArrayList<>();
                oos.writeObject(list);
                oos.close();
            }
        }
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
        list = (ArrayList<String>) ois.readObject();
        if (msg != null && !msg.equals("")) {
            if (list.size() >= 100) {
                list.remove(0);
            }
            list.add(msg);
        }
        ois.close();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
        oos.writeObject(list);
        oos.close();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(list);
    }

    /**
     * 初始化相册
     *
     * @param clubid
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "initalbum")
    @ResponseBody
    public String initalbum(String clubid) throws JsonProcessingException {
        String filepath = clubsServices.getalbum(clubid);
        System.out.println(filepath);
        if (filepath == null || filepath.length() == 0) {
            filepath = "D:/clubtribefile/clubalbum/album" + clubid;
            new File(filepath).mkdir();
            Club club = new Club();
            club.setClubid(clubid);
            club.setAlbum(filepath);
            clubsServices.initalbum(club);
        }
        filepath = clubsServices.getalbum(clubid);
        File dir = new File(filepath);
        String[] files = dir.list();
        if (files.length == 0) {
            return "false";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(files);
    }

    /**
     * 上传图片
     *
     * @param request
     * @param clubid
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/uploadFiles", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String uploadFiles(HttpServletRequest request, String clubid) throws IOException {
        String savePath = clubsServices.getalbum(clubid);
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        MultipartFile file = null;
        BufferedOutputStream stream = null;
        for (int i = 0; i < files.size(); ++i) {
            file = files.get(i);
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();
                    File saveFile = new File(savePath, file.getOriginalFilename());
                    stream = new BufferedOutputStream(new FileOutputStream(saveFile));
                    stream.write(bytes);
                    stream.close();
                } catch (Exception e) {
                    if (stream != null) {
                        stream.close();
                        stream = null;
                    }
                    return "第 " + i + " 个文件上传有错误" + e.getMessage();
                }
            } else {
                return "第 " + i + " 个文件为空";
            }
        }
        return "所有文件上传成功";
    }

    /**
     * 上传文件
     *
     * @param request
     * @param clubid
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "uploadshareFiles", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String uploadshareFiles(HttpServletRequest request, String clubid) throws IOException {
        String savePath = clubsServices.getsharefile(clubid);
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        MultipartFile file = null;
        BufferedOutputStream stream = null;
        for (int i = 0; i < files.size(); ++i) {
            file = files.get(i);
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();
                    File saveFile = new File(savePath, file.getOriginalFilename());
                    stream = new BufferedOutputStream(new FileOutputStream(saveFile));
                    stream.write(bytes);
                    stream.close();
                } catch (Exception e) {
                    if (stream != null) {
                        stream.close();
                        stream = null;
                    }
                    return "第 " + i + " 个文件上传有错误" + e.getMessage();
                }
            } else {
                return "第 " + i + " 个文件为空";
            }
        }
        return "所有文件上传成功";
    }

    /**
     * 初始化公告
     *
     * @param clubid
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping(value = "initnotice", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String initnotice(String clubid) throws IOException, ClassNotFoundException {
        String filepath = clubsServices.getnotice(Integer.parseInt(clubid));
        ArrayList<String> list = null;
        if (filepath == null || filepath.length() == 0) {
            filepath = "D:/clubtribefile/clubmsg/notice" + clubid + ".txt";
            File file = new File(filepath);
            if (!file.exists()) {
                file.createNewFile();
                Club club = new Club();
                club.setNotice(file.toString());
                club.setClubid(clubid);
                clubsServices.initnotice(club);
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
                list = new ArrayList<>();
                oos.writeObject(list);
                oos.close();
            }
        } else {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
            list = (ArrayList<String>) ois.readObject();
            ois.close();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(list);
    }

    /**
     * 发布公告
     *
     * @param clubid
     * @param notice
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping(value = "notice")
    @ResponseBody
    public void notice(String clubid, String notice) throws IOException, ClassNotFoundException {
        String filepath = clubsServices.getnotice(Integer.parseInt(clubid));
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
        ArrayList<String> list = (ArrayList<String>) ois.readObject();
        if (notice != null && !notice.equals("")) {
            list.add(notice);
        }
        ois.close();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
        oos.writeObject(list);
        oos.close();
    }

    @RequestMapping(value = "removenotice")
    @ResponseBody
    public void removenotice(String clubid, String notice) throws IOException, ClassNotFoundException {
        String filepath = clubsServices.getnotice(Integer.parseInt(clubid));
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
        ArrayList<String> list = (ArrayList<String>) ois.readObject();
        if (notice != null && !notice.equals("")) {
            notice = "<li>" + notice + "</li>";
            list.remove(notice);
        }
        ois.close();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath));
        oos.writeObject(list);
        oos.close();
    }

    /**
     * 初始化分享文件
     *
     * @param clubid
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "initsharefile")
    @ResponseBody
    public String initsharefile(String clubid) throws JsonProcessingException {
        String filepath = clubsServices.getsharefile(clubid);
        if (filepath == null || filepath.length() == 0) {
            filepath = "D:\\clubtribefile\\clubsharefile\\files" + clubid;
            new File(filepath).mkdir();
            Club club = new Club();
            club.setClubid(clubid);
            club.setSharefile(filepath);
            clubsServices.initsharefile(club);
        }
        filepath = clubsServices.getsharefile(clubid);
        File dir = new File(filepath);
        String[] files = dir.list();
        if (files.length == 0) {
            return "false";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(files);
    }

    /**
     * filedownload
     *
     * @param
     * @param
     * @return
     */
    @RequestMapping("filedownload")
    public ResponseEntity<byte[]> download(String filename, String clubid) throws IOException {
        String dfileName = new String(filename.getBytes("gb2312"), "iso8859-1");
        File file = new File(clubsServices.getsharefile(clubid) + "\\" + filename);
        System.out.println(file.toString());
        if (file.exists()) {
            System.out.println("exists");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", dfileName);
        return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file), headers, HttpStatus.CREATED);
    }

    /**
     * 创建活动
     *
     * @param clubid
     * @param aname
     * @param begintime
     * @param endtime
     * @param atype
     * @param aplace
     * @param itrdc
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping("createNewActivity")
    @ResponseBody
    public String createNewActivity(String clubid, String aname, String begintime, String endtime, String atype, String aplace, String itrdc) throws IOException, ClassNotFoundException {
        String cn=clubsServices.findnamebyid(Integer.parseInt(clubid));
        String sid=clubsServices.getschoolid(Integer.parseInt(clubid));
        String sn=clubsServices.getsnamebyid(sid);
        System.out.println(cn +"@"+ sn);
        String filepath = "D:\\clubtribefile\\activatyfile";
        File file = new File(filepath + "\\activity.txt");
        Map<Integer, ArrayList<String>> map = new TreeMap<>();
        if (!file.exists()) {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(map);
            oos.close();
        }
        Integer id = Integer.parseInt(new Generator().gRanId());
        ArrayList<Integer> ids = activityServices.getids();
        while (ids.contains(id)) {
            id = Integer.parseInt(new Generator().gRanId());
        }
        ArrayList<String> member = new ArrayList<>();
        Activity activity = new Activity(clubid, id, aname, begintime, endtime, 1, member, aplace, itrdc,sn ,cn,Integer.parseInt(atype));
        System.out.println(activity.toString());
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        map = (Map<Integer, ArrayList<String>>) ois.readObject();
        ois.close();
        map.put(id, member);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(map);
        oos.close();
        if (activityServices.insertNewActivity(activity) != 0) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString("创建成功!");
        }
        return null;
    }

    /**
     * 获取所有活动
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping("getallactivity")
    @ResponseBody
    public String getallactivity() throws IOException, ClassNotFoundException {
        ArrayList<Activity> acs = activityServices.getallactivity();
        ObjectInputStream oos = new ObjectInputStream(new FileInputStream("D:\\clubtribefile\\activatyfile\\activity.txt"));
        Map<Integer, ArrayList<String>> map = (Map<Integer, ArrayList<String>>) oos.readObject();
        oos.close();
        for (Activity it : acs) {
            it.setMemberid(map.get(it.getId()));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(acs);
    }

    /**
     * 加入活动
     *
     * @param id
     * @param userid
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @RequestMapping("joinactivity")
    @ResponseBody
    public String joinactivity(String id, String userid) throws IOException, ClassNotFoundException {
        String result = "加入成功!";
        boolean ifjoin = false;
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("D:\\clubtribefile\\activatyfile\\activity.txt"));
        Map<Integer, ArrayList<String>> map = (Map<Integer, ArrayList<String>>) ois.readObject();
        ois.close();
        for (String it : map.get(Integer.parseInt(id))) {
            if (!userid.equals(it)) {
                ifjoin = true;
            }
        }
        if (ifjoin || map.get(Integer.parseInt(id)).size() == 0) {
            map.get(Integer.parseInt(id)).add(userid);
        } else {
            result = "请不要重复加入!";
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("D:\\clubtribefile\\activatyfile\\activity.txt"));
        oos.writeObject(map);
        return objectMapper.writeValueAsString(result);
    }
}
