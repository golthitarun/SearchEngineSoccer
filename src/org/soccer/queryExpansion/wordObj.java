package org.soccer.queryExpansion;

public class wordObj{
    String u;
    String v;
    double val;

    public wordObj(String u, String v, double val){
        this.u = u;
        this.v = v;
        this.val = val;
    }

    public String getWordObjString() {
        return this.u+":"+this.v+":"+this.val;
    }

}
