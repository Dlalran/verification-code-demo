package prvi.howard.lettucedemo.entity;

import lombok.Data;

@Data
public class CodeResponse {
    /**
     * @Description 验证码接口返回信息实体类
     */
    private int code;
    private String smsid;
    private String msg;
}
