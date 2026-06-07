package us.cubk.openajs.api.model;

import lombok.Data;

import java.util.List;

@Data
public class VpnServer {

    private String id;
    private int vsid;
    private String ctry;
    private String svr;
    private String tit;
    private String cmt;
    private String sts;
    private String ord;
    private List<String> cat;
    private Par par;
    private List<Loc> loc;

    public boolean isOk() {
        return "ok".equals(sts);
    }

    public int port() {
        return par != null && par.fv != null ? par.fv.p : 0;
    }

    public int uid() {
        return par != null && par.fv != null ? par.fv.u : 0;
    }

    public String secret() {
        return par != null && par.fv != null ? par.fv.ps : null;
    }

    @lombok.Data
    public static class Par {
        private Fv fv;
    }

    @lombok.Data
    public static class Fv {
        private int u;
        private int p;
        private String ps;
    }

    @lombok.Data
    public static class Loc {
        private String c;
        private String n;
        private int o;
    }
}
