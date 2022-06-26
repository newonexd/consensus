package org.newonexd.raft.common.msg;

import java.io.Serializable;

public enum MessageType implements Serializable {
    HEART_BEAT,
    REQUEST,
    RESPONSE;
}
