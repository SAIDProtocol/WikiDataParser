package edu.rutgers.winlab.topologyhandler;

/**
 *
 * @author jiachen
 */
public class Node {

    public enum NodeType {
        Border,
        Core,
        Edge;

        public static NodeType fromString(String str) {
            switch (str) {
                case "B":
                    return NodeType.Border;
                case "C":
                    return NodeType.Core;
                case "E":
                    return NodeType.Edge;
                default:
                    return NodeType.fromString(str);
            }
        }

        public String getShortString() {
            switch (this) {
                case Border:
                    return "B";
                case Core:
                    return "C";
                case Edge:
                    return "E";
                default:
                    return toString();
            }
        }
    }

    private final String name;
    private final String city;
    private final NodeType type;

    public Node(String name, String city, NodeType type) {
        this.name = name;
        this.city = city;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public NodeType getType() {
        return type;
    }

}
