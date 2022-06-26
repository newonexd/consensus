package org.newonexd.raft.common.node;

import org.apache.commons.lang3.time.StopWatch;
import org.newonexd.raft.core.Server;

import java.io.Serializable;

public class Node implements Serializable {

    public Node(String nodeId) {
        this.nodeId = nodeId;
        this.nodeType = NodeType.CANDIDATE;
        this.term = 1;
        this.stopWatch = new StopWatch();
        this.server = new Server();
    }

    private String nodeId;

    private NodeType nodeType;

    private int term;

    private StopWatch stopWatch;

    private Server server;

    public void start(){
        this.server.start();
        this.stopWatch.start();
    }
}
