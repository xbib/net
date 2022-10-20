package org.xbib.net;

public interface Context<Req, Resp> {

    Req request();

    Resp response();
}
