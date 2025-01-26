package aicreative.ai.controlplane.api.common;


import lombok.Data;
import lombok.Getter;

@Data
public class Response<T> {
    private String code;
    private String message;
    private T data;

    public Response(ResponseCode code, T data) {
        this.code = code.getCode();
        this.data = data;
    }

    public Response(ResponseCode code, String message, T data) {
        this.code = code.getCode();
        this.message = message;
        this.data = data;
    }

    @Getter
    public enum ResponseCode {
        Success("00000"),
        ClientError("A0001"),
        IllegalParam("A0400"),
        ServiceError("B0001"),
        BackendError("C0001");

        private final String code;

        ResponseCode(String code) {
            this.code = code;
        }
    }

}
