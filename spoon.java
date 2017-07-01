import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.math.*;

/**
 * The machines are gaining ground. Time to show them what we're really made of...
 **/
class Player {
    static final int TOP    = 0;
    static final int LEFT   = 1;
    static final int BOTTOM = 2;
    static final int RIGHT  = 3;

    static final int[] oppositeDirections = new int[] {BOTTOM, RIGHT, TOP, LEFT};

    static int getOppositeDirection(final int direction) {
        return oppositeDirections[direction];
    }

    static class Link {
        public Link(final Node from, final Node to, final int dir) {
            this.from = from;
            this.to = to;
            this.dir = dir;
        }

        public final Node from;
        public final Node to;
        public final int dir;
    }

    static class Node {
        public Node(final int x, final int y, final int v) {
            this.id = nextId++;
            this.x = x;
            this.y = y;
            this.target = v;
        }

        public void link(final int direction, final Node other) {
            links[direction] = other;
            other.links[getOppositeDirection(direction)] = this;
        }

        public void getChoicesFromNode() {
            iter = 0;
            choices = 
                Stream.of(links)
                    .filter(n -> n != null)
                    .map(n -> new Link(this, n, iter++))
                    .sorted( (a, b) -> b.to.target - a.to.target)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public final int id;
        public final int x;
        public final int y;
        public final int target;
        public final Node[] links = new Node[] {null, null, null, null};
        public int current = 0;
        public ArrayList<Link> choices;

        private int iter = 0;
        private static int nextId = 0;
    }
    
    static class Graph {
        public int total = 0;
        public ArrayList<Node> nodes = new ArrayList<>();
        
        public static Graph buildGraphFromInput() {
            final Graph graph = new Graph();
            final Scanner in = new Scanner(System.in);
            
            final int width = in.nextInt(); // the number of cells on the X axis
            System.err.println(String.format("IN %d", width));
            final int height = in.nextInt(); // the number of cells on the Y axis
            System.err.println(String.format("IN %d", height));
            final Node[] markersW = new Node[width];
            if (in.hasNextLine()) {
                in.nextLine();
            }
            for (int i = 0; i < height; i++) {
                Node markerH = null;
                String line = in.nextLine(); // width characters, each either a number or a '.'
                System.err.println(String.format("IN %s", line));
                for (int j = 0; j < line.length(); ++j) {
                    final char c = line.charAt(j);

                    if (c == '.') {
                        continue; // <== 
                    }
                    final int value = c - '0';
                    graph.total += value;
                    final Node node = new Node(j, i, value);
                    graph.nodes.add(node);
                    if (markerH != null) {
                        markerH.link(RIGHT, node);
                    }
                    markerH = node;
                    if (markersW[j] != null) {
                        markersW[j].link(BOTTOM, node);
                    }
                    markersW[j] = node;
                }
            }

            graph.buildChoices();

            graph.nodes = graph.nodes
                .stream()
                .sorted( (a, b) -> b.target - a.target)
                .collect(Collectors.toCollection(ArrayList::new));

            return graph;
        }
        
        public void buildChoices() {
            nodes.stream().forEach(Node::getChoicesFromNode);
        }
    }

    private final Graph graph;
    private final boolean[] activeNodes;
    private int ctxtNextId = 0;
    private final Link[] choices = new Link[31*31*4];

    final int[] stackId = new int[1000];
    final Link[] stackOp = new Link[1000];
    final Node[] stackNode = new Node[1000];
    final int[] stackStartPos = new int[1000];
    final int[] stackStopPos = new int[1000];
    int sp = -1;

    public Player(final Graph graph) {
        this.graph = graph;
        activeNodes = new boolean[graph.nodes.size()];
        for (int i = 0; i < activeNodes.length; ++i) {
            activeNodes[i] = false;
        }
    }

    private boolean isNodeInStack(final Node node) {
        return activeNodes[node.id];
    }

    private void dumpStack() {
        final HashMap<Integer, Integer> res = new HashMap<>();
        for (int i = 1; i <= sp; ++i) {
            final Link link = stackOp[i];
            final int minX = Math.min(link.from.x, link.to.x);
            final int maxX = Math.max(link.from.x, link.to.x);
            final int minY = Math.min(link.from.y, link.to.y);
            final int maxY = Math.max(link.from.y, link.to.y);
            final int k = (minY * 40 + minX) * 10000 + (maxY * 40 + maxX);
            if (res.get(k) != null) {
                res.replace(k, 2);
            }
            else {
                res.put(k, 1);
            }
        }

        res.entrySet().stream().forEach(e -> {
            int k = e.getKey();
            final int minX = (k / 10000) % 40;
            final int minY = (k / 10000) / 40;
            final int maxX = (k % 10000) % 40;
            final int maxY = (k % 10000) / 40;
            System.out.println(String.format("%d %d %d %d %d", minX, minY, maxX, maxY, e.getValue()));
        });
    }

    boolean crossH(final Link operation) {
        for (int i = 1; i <= sp; ++i) {
            final Link op = stackOp[i];
            if (op.from.y != op.to.y) {
                continue; // <==  not an horizontal
            }
            final int refY = op.from.y;
            final int minY = Math.min(operation.from.y, operation.to.y);
            final int maxY = Math.max(operation.from.y, operation.to.y);
            if (refY <= minY || refY >= maxY) {
                continue; // <==
            }

            final int refX = operation.from.x;
            final int minX = Math.min(op.from.x, op.to.x);
            final int maxX = Math.max(op.from.x, op.to.x);
            if (refX <= minX || refX >= maxX) {
                continue; // <==
            }

            return true; // <== found one edge crossing
        }
        return false;
    }

    boolean crossV(final Link operation) {
        for (int i = 1; i <= sp; ++i) {
            final Link op = stackOp[i];
            if (op.from.x != op.to.x) {
                continue; // <==  not an horizontal
            }

            final int refX = op.from.x;
            final int minX = Math.min(operation.from.x, operation.to.x);
            final int maxX = Math.max(operation.from.x, operation.to.x);
            if (refX <= minX || refX >= maxX) {
                continue; // <==
            }

            final int refY = operation.from.y;
            final int minY = Math.min(op.from.y, op.to.y);
            final int maxY = Math.max(op.from.y, op.to.y);
            if (refY <= minY || refY >= maxY) {
                continue; // <==
            }

            return true; // <== found one edge crossing
        }
        return false;
    }

    boolean cross(final Link operation) {
        final int dir = operation.dir;
        if (dir == TOP || dir == BOTTOM) {
            return crossH(operation);
        }
        return crossV(operation);
    }

    boolean commitOperation(final Link link) {
        if (link.from.current == link.from.target || link.to.current == link.to.target) {
            return false; // <== source and/or target node are "full"
        }

        if (cross(link)) {
            return false; // <== operation would cross a edge
        }

        ++link.from.current;
        ++link.to.current;

        return true;
    }

    void rollbackOperation(final Link link) {
        --link.from.current;
        --link.to.current;
    }

    private int copyChoices(final ArrayList<Link> from, final int pos) {
        int i = 0;
        for (; i < from.size(); ++i) {
            choices[pos+i] = from.get(i);
            // System.err.println(String.format("Add route [%d] -> [%d]", choices[pos+i].from.id, choices[pos+i].to.id));
        }
        return pos+i;
    }

    private String dumpChoices() {
        StringBuilder res = new StringBuilder();
        res.append("Choices [");
        for (int i  = 0; i < stackStopPos[sp]; ++i) {
            if (i == stackStartPos[sp])  {
                res.append("*");
            }
            res
                .append("(")
                .append(choices[i].from.id)
                .append(" -> ")
                .append(choices[i].to.id)
                .append(")");
        }
        res.append("]");
        return res.toString();
    }

    public void play()  {
        int total = graph.total;
        int maxSp = 0;

        final long startTime = System.nanoTime();
        for (int i = 0; i < graph.nodes.size(); ++i) {
            final Node root = graph.nodes.get(i);
            // System.err.println("root node " + root.id);
            final int stopPos = copyChoices(root.choices, 0);
            activeNodes[root.id] = true;
            ++sp;
            stackId[sp] = ctxtNextId++;
            stackOp[sp] = null;
            stackNode[sp] = root;
            stackStartPos[sp] = 0;
            stackStopPos[sp] = stopPos;
            while(total > 0 && sp >= 0) {
                // System.err.println(String.format("%d - %d - Loop - Node [%d] Range [%d -> %d] %s", stackId[sp], sp, stackNode[sp] != null ? stackNode[sp].id : -1, stackStartPos[sp], stackStopPos[sp], dumpChoices()));
                if (stackStartPos[sp] == stackStopPos[sp]) {
                    if (stackOp[sp] != null) {
                        // System.err.println(String.format("%d - Node [%d] Rollback [%d -> %d]", stackId[sp], stackNode[sp] != null ? stackNode[sp].id : -1, stackOp[sp].from.id, stackOp[sp].to.id));
                        rollbackOperation(stackOp[sp]);
                        total += 2;
                    }

                    if (stackNode[sp] != null) {
                        // System.err.println(String.format("%d - unmark node [%d]", stackId[sp], stackNode[sp].id));
                        activeNodes[stackNode[sp].id] = false;
                    }

                    --sp;
                    continue; // <==
                }

                final Link choice = choices[stackStartPos[sp]++];
                if (!commitOperation(choice)) {
                    // System.err.println(String.format("Drop route [%d] -> [%d]", choice.from.id, choice.to.id));
                    continue; // <==
                }
                // System.err.println(String.format("Use route [%d] -> [%d]", choice.from.id, choice.to.id));

                final Node target = choice.to;
                Node nextNode = null;
                int newStopPos;
                if (isNodeInStack(target)) {
                    newStopPos = stackStopPos[sp];
                }
                else {
                    // System.err.println("mark node " + target.id);
                    nextNode = target;
                    newStopPos = copyChoices(target.choices, stackStopPos[sp]);
                    activeNodes[target.id] = true;
                }
                total -= 2;
                ++sp;
                maxSp = Math.max(maxSp, sp);
                stackId[sp] = ctxtNextId++;
                stackOp[sp] = choice;
                stackNode[sp] = nextNode;
                stackStartPos[sp] = stackStartPos[sp-1];
                stackStopPos[sp] = newStopPos;
            }

            if (total == 0) {
                // System.err.println("Max SP " + maxSp);
                // System.err.println("Elapsed " + (System.nanoTime() - startTime)/1000000.0);
                dumpStack();
                break; // <==
            }
            activeNodes[root.id] = false;
            --sp;
        }
    }

    public static void main(String args[]) {
        final Graph graph = Graph.buildGraphFromInput();
        final Player player = new Player(graph);
        player.play();
    }
}
