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
            final int height = in.nextInt(); // the number of cells on the Y axis
            final Node[] markersW = new Node[width];
            if (in.hasNextLine()) {
                in.nextLine();
            }
            for (int i = 0; i < height; i++) {
                Node markerH = null;
                String line = in.nextLine(); // width characters, each either a number or a '.'
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

    static class Context {
        public Context(final int id, final Link op, final Node node, final int startPos, final int stopPos) {
            this.id = id;
            this.op = op;
            this.node = node;
            this.startPos = startPos;
            this.stopPos = stopPos;
        }

        public final int id;
        public final Link op;
        public final Node node;
        public int startPos;
        public final int stopPos;
    }

    private final Graph graph;
    private final boolean[] activeNodes;
    private int ctxtNextId = 0;
    private final Stack<Context> stack = new Stack<Context>();
    private final Link[] choices = new Link[31*31*4];

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
        for (int i = 1; i < stack.size(); ++i) {
            final Link link = stack.get(i).op;
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
        for (int i = 1; i < stack.size(); ++i) {
            final Link op = stack.get(i).op;
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
        for (int i = 1; i < stack.size(); ++i) {
            final Link op = stack.get(i).op;
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
        }
        return pos+i;
    }

    public void play()  {
        int totalCtxts = 0;
        int total = graph.total;
        final long startTime = System.nanoTime();
        for (int i = 0; i < graph.nodes.size(); ++i) {
            final Node root = graph.nodes.get(i);
            final int stopPos = copyChoices(root.choices, 0);
            activeNodes[root.id] = true;
            stack.push(new Context(ctxtNextId++, null, root, 0, stopPos));
            ++totalCtxts;
            while(total > 0 && !stack.empty()) {
                final Context ctxt = stack.peek();

                if (ctxt.startPos == ctxt.stopPos) {
                    if (ctxt.op != null) {
                        rollbackOperation(ctxt.op);
                        total += 2;
                    }

                    if (ctxt.node != null) {
                        activeNodes[ctxt.node.id] = false;
                    }

                    stack.pop();
                    continue; // <==
                }

                final Link choice = choices[ctxt.startPos++];
                if (!commitOperation(choice)) {
                    continue; // <==
                }

                final Node target = choice.to;
                Node nextNode = null;
                int newStopPos;
                if (isNodeInStack(target)) {
                    newStopPos = ctxt.stopPos;
                }
                else {
                    nextNode = target;
                    newStopPos = copyChoices(target.choices, ctxt.stopPos);
                    activeNodes[target.id] = true;
                }
                total -= 2;
                ++totalCtxts;
                stack.push(new Context(ctxtNextId++, choice, nextNode, ctxt.startPos, newStopPos));
            }

            if (total == 0) {
                System.err.println("Contextes " + totalCtxts);
                System.err.println("Elapsed " + (System.nanoTime() - startTime));
                dumpStack();
                break; // <==
            }
            activeNodes[root.id] = false;
            stack.pop();
        }
    }

    public static void main(String args[]) {
        final Graph graph = Graph.buildGraphFromInput();
        final Player player = new Player(graph);
        player.play();
    }
}
