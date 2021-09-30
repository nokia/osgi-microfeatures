package com.alcatel.as.diameter.lb;

public class DiameterException extends RuntimeException {

    public static final DiameterException DIAMETER_COMMAND_UNSUPPORTED = new DiameterException (3001);
    public static final DiameterException DIAMETER_UNABLE_TO_DELIVER = new DiameterException (3002);
    public static final DiameterException DIAMETER_TOO_BUSY = new DiameterException (3004);
    public static final DiameterException DIAMETER_APPLICATION_UNSUPPORTED = new DiameterException (3007);

    public static final DiameterException DIAMETER_MISSING_AVP = new DiameterException (5005);
    
    private DiameterUtils.Avp _codeAvp;
    private int _code;

    public DiameterException (int code){
	this (code, DiameterUtils.setIntValue (code, new byte[4], 0));
    }
    public DiameterException (int code, byte[] codeBytes){
	this (code, new DiameterUtils.Avp (268, 0, true, codeBytes));
    }
    public DiameterException (int code, DiameterUtils.Avp codeAvp){
	_code = code;
	_codeAvp = codeAvp;
    }

    public int getCode (){ return _code;}

    public DiameterMessage makeResponse (DiameterMessage request){
	return DiameterUtils.makeResponse (request, _codeAvp);
    }
    public DiameterMessage makeResponse (DiameterMessage request, DiameterUtils.Avp extraAvp){
	return DiameterUtils.makeResponse (request, _codeAvp, extraAvp);
    }
}
