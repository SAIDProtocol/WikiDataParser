package edu.rutgers.winlab.topologyhandler;

import java.util.Objects;

/**
 *
 * @author jiachen
 */
public class Link {

    private final Node n1, n2;
    private final int latencyMs;

    public Link(Node n1, Node n2, int latencyMs) {
        this.n1 = n1;
        this.n2 = n2;
        this.latencyMs = latencyMs;
    }

    public Node getN1() {
        return n1;
    }

    public Node getN2() {
        return n2;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.n1);
        hash = 29 * hash + Objects.hashCode(this.n2);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Link other = (Link) obj;
        if (!Objects.equals(this.n1, other.n1)) {
            return false;
        }
        return Objects.equals(this.n2, other.n2);
    }

}
