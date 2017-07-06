import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
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
            this.id = nextId++;
            this.from = from;
            this.to = to;
            this.dir = dir;
        }

        public void crossAndMark(final Link operation) {
            if (cross(operation)) {
                System.err.println(String.format("Detected cross [%d] X [%d]", id, operation.id));
                if (! crosses.contains(operation.id)) {
                    crosses.add(operation.id);
                }
                if (! operation.crosses.contains(id)) {
                    operation.crosses.add(id);
                }
            }
        }

        private boolean cross(final Link operation) {
            final int dirOther = operation.dir;
            if (dir == dirOther || dir == getOppositeDirection(dirOther)) {
                return false; // <== 
            }

            if (dir == TOP || dir == BOTTOM) {
                return crossH(operation); // <== 
            }
            return crossV(operation);
        }

        private boolean crossH(final Link other) {
            if (other.from.y != other.to.y) {
                return false; // <==  not an horizontal
            }

            final int refY = other.from.y;
            final int minY = Math.min(from.y, to.y);
            final int maxY = Math.max(from.y, to.y);
            if (refY <= minY || refY >= maxY) {
                return false; // <==
            }

            final int refX = from.x;
            final int minX = Math.min(other.from.x, other.to.x);
            final int maxX = Math.max(other.from.x, other.to.x);
            if (refX <= minX || refX >= maxX) {
                return false; // <==
            }

            return true; // <== found one edge crossing
        }

        private boolean crossV(final Link other) {
            if (other.from.x != other.to.x) {
                return false; // <==  not an vertical
            }

            final int refX = other.from.x;
            final int minX = Math.min(from.x, to.x);
            final int maxX = Math.max(from.x, to.x);
            if (refX <= minX || refX >= maxX) {
                return false; // <==
            }

            final int refY = from.y;
            final int minY = Math.min(other.from.y, other.to.y);
            final int maxY = Math.max(other.from.y, other.to.y);
            if (refY <= minY || refY >= maxY) {
                return false; // <==
            }

            return true; // <== found one edge crossing
        }

        public final int id;
        public final Node from;
        public final Node to;
        public final int dir;
        public final ArrayList<Integer> crosses = new ArrayList<>();

        private static int nextId = 0;
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
            iter = -1;
            choices = 
                Stream.of(links)
                    .filter(n -> {
                        ++iter;
                        return n != null;
                    })
                    .map(n -> new Link(this, n, iter))
                    .sorted( (a, b) -> b.to.target - a.to.target)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public Link removeChoice(final Node to) {
            for (int i = 0; i < choices.size(); ++i) {
                if (choices.get(i).to == to) {
                    return choices.remove(i); // <== 
                }
            }
            return null;
        }

        public Link removeChoice(final Link link) {
            for (int i = 0; i < choices.size(); ++i) {
                if (choices.get(i) == link) {
                    return choices.remove(i); // <== 
                }
            }
            return null;
        }

        public void removeAllChoices() {
            choices.clear();
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
        public List<Node> nodes = new ArrayList<>();
        public Map<Integer, Link> links = new HashMap<>();
        public ArrayList<Link> obvious = new ArrayList<>();
        public int width = 0;
        public int height = 0;

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
            graph.width = width;
            graph.height = height;

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

            System.err.println(String.format("Initial total %d", graph.total));
            System.err.println(String.format("Initial node count %d", graph.nodes.size()));
            graph.buildChoices();
            System.err.println(String.format("Initial links count %d", graph.links.size()));
            graph.buildCrosses();

            graph.dump();
            
            for (;;) {
                final ArrayList<Link> res = graph.findObviousCases();
                if (res.size() == 0) {
                    break; // <== 
                }
                graph.cleanFullNodes();
                graph.removeIsolatedNodes();
            }
            graph.sortNodes();

            System.err.println(String.format("Optimized total %d", graph.total));
            System.err.println(String.format("Optimized node count %d", graph.nodes.size()));
            return graph;
        }

        public void dump() {
            System.err.println("* Nodes");
            nodes.forEach(n -> {
                n.choices.stream().forEach(l -> {
                    StringBuilder crosses = new StringBuilder();
                    for (int i : l.crosses) {
                        crosses.append(i);
                        crosses.append(", ");
                    }
                    System.err.println(String.format("Node [%d] | Link [%d] -> Node [%d] | %s", n.id, l.id, l.to.id, crosses.toString()));
                });
            });

            System.err.println("* Links");
            links.forEach((k, l) -> {
                StringBuilder crosses = new StringBuilder();
                for (int i : l.crosses) {
                    crosses.append(i);
                    crosses.append(", ");
                }
                System.err.println(String.format("Link [%d] | From [%d] -> to [%d] | %s", l.id, l.from.id, l.to.id, crosses.toString()));
            });
        }

        private void buildCrosses() {
            for (int i = 0; i < links.size(); ++i) {
                final Link a = links.get(i);
                for (int j = 0; j < links.size(); ++j) {
                    if (i == j) {
                        continue; // <== 
                    }
                   final Link b = links.get(j);
                   a.crossAndMark(b);
                }
            }
        }

        private void buildChoices() {
            nodes.forEach(e -> {
                e.getChoicesFromNode();
                e.choices.stream().forEach(l -> {
                    links.put(l.id, l);
                });
            });
        }
        
        private ArrayList<Link> tryIsolatedOneNode(final Node e) {
            ArrayList<Link> res = new ArrayList<>();

            final int delta = (e.target - e.current);
            if (delta == 1 && e.choices.size() == 1) {
                total -= 2;
                System.err.println(String.format("Found isolated one node [%d] with value [%d]", e.id, delta));
                final Link linkOut = e.choices.get(0);
                e.choices.clear();
                System.err.println(String.format("-> Isolated one node | [%d] remove link [%d]", e.id, linkOut.id));
                res.add(linkOut);
                linkOut.from.current++;
                linkOut.to.current++;

                final Link ol = linkOut.to.removeChoice(e);
                System.err.println(String.format("<- Isolated one node | [%d] remove link [%d]", linkOut.to.id, ol.id));
            }            

            res.stream().forEach(l -> {
                l.crosses.forEach(lid -> {
                    final Link cross = links.get(lid);
                    final Node node = cross.from;
                    for (int i = 0; i < node.choices.size(); ++i) {
                        final Link link = node.choices.get(i);
                        if (link == cross) {
                            System.err.println(String.format("Link [%d] crosses [%d] ([%d] -> [%d])", l.id, lid, cross.from.id, cross.to.id));
                            node.removeChoice(link);
                            break; // <== 
                        }
                    }
                });
            });

            return res;
        }
        
        private ArrayList<Link> tryFullNodeCase(final Node e) {
            ArrayList<Link> res = new ArrayList<>();

            final int delta = (e.target - e.current);
            if (delta > 0 && delta == e.choices.size()*2) {
                System.err.println(String.format("Full node [%d] with value [%d]", e.id, delta));
                total -= delta*2;
                e.choices.stream().forEach(l -> {
                    System.err.println(String.format("-> Full node | [%d] remove link [%d]", e.id, l.id));
                    res.add(l);
                    l.from.current++;
                    l.to.current++;

                    final Link ol = l.to.removeChoice(e);
                    System.err.println(String.format("<- Full node | [%d] remove link [%d]", l.to.id, ol.id));
                    res.add(ol);
                    ol.from.current++;
                    ol.to.current++;
                });
                e.removeAllChoices();
            }

            res.stream().forEach(l -> {
                l.crosses.forEach(lid -> {
                    final Link cross = links.get(lid);
                    final Node node = cross.from;
                    for (int i = 0; i < node.choices.size(); ++i) {
                        final Link link = node.choices.get(i);
                        if (link == cross) {
                            System.err.println(String.format("Link [%d] crosses [%d] ([%d] -> [%d])", l.id, lid, cross.from.id, cross.to.id));
                            node.removeChoice(link);
                            break; // <== 
                        }
                    }
                });
            });

            return res;
        }
        private ArrayList<Link> findObviousCases() {
            List<Function<Node, ArrayList<Link>>> heuristics = Arrays.asList(this::tryFullNodeCase, this::tryIsolatedOneNode);
            ArrayList<Link> res = new ArrayList<>();

            for (int i = 0; i < nodes.size(); ++i) {
                final Node e = nodes.get(i);
                for (Function<Node, ArrayList<Link>> f : heuristics) {
                    ArrayList<Link> links = f.apply(e);
                    if (links.size() > 0) {
                        res.addAll(links);
                        break; // <== 
                    }
                }
            }

            obvious.addAll(res);
            return res;
        }

        void cleanFullNodes() {
            nodes
                .stream()
                .filter(n -> (n.target - n.current) == 0 && n.choices.size() > 0)
                .forEach(n -> {
                    System.err.println(String.format("Clean empty node [%d]", n.id));
                    n.choices.stream().forEach(l -> {
                        System.err.println(String.format("-> Clean node [%d] remove link [%d]", n.id, l.id));
                        final Link ol = l.to.removeChoice(n);
                        System.err.println(String.format("<- Clean [%d] remove link [%d]", l.to.id, ol.id));
                    });
                    n.removeAllChoices();
                });
        }

        void removeIsolatedNodes() {
            nodes = nodes
                .stream()
                .filter(n -> {
                    final boolean r = (n.target - n.current) > 0;
                    if (! r) {
                        System.err.println(String.format("Remove node [%d]", n.id));
                    }
                    return r;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        }

        private void sortNodes() {
            nodes = nodes
                .stream()
                .sorted((a, b) -> b.target - a.target)
                .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private final Graph graph;
    private final boolean[] activeNodes;
    private final boolean[] activeLinks;
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
        activeNodes = new boolean[graph.width * graph.height];
        for (int i = 0; i < activeNodes.length; ++i) {
            activeNodes[i] = false;
        }
        activeLinks = new boolean[graph.width * graph.height * 4];
        for (int i = 0; i < activeLinks.length; ++i) {
            activeLinks[i] = false;
        }

        // graph.obvious.stream().forEach(l -> {
        //     activeLinks[l.id] = true;
        // });
    }

    private boolean isNodeInStack(final Node node) {
        return activeNodes[node.id];
    }

    private void dumpStack() {
        final HashMap<Integer, Integer> res = new HashMap<>();
        System.err.println(String.format("Obvious %d", graph.obvious.size()));
        for (int i = 0; i < graph.obvious.size(); ++i) {
            final Link link = graph.obvious.get(i);
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

    boolean cross(final Link operation) {
        for (int i = 0; i < operation.crosses.size(); ++i) {
            if (activeLinks[operation.crosses.get(i)]) {
                return true;
            }
        }
        return false;
    }

    boolean commitOperation(final Link link) {
        if (link.from.current == link.from.target || link.to.current == link.to.target) {
            return false; // <== source and/or target node are "full"
        }

        if (link.crosses.size() > 0 && cross(link)) {
            return false; // <== operation would cross a edge
        }

        activeLinks[link.id] = true;
        ++link.from.current;
        ++link.to.current;

        return true;
    }

    void rollbackOperation(final Link link) {
        --link.from.current;
        --link.to.current;
        activeLinks[link.id] = false;
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

        if (total == 0) {
            dumpStack();
        }
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
        }
    }

    public static void main(String args[]) {
        final long graphStartTime = System.nanoTime();
        final Graph graph = Graph.buildGraphFromInput();
        System.err.println(String.format("Build graph in : %f ms", (System.nanoTime()-graphStartTime)/1000000.0));

        final long solveStartTime = System.nanoTime();
        final Player player = new Player(graph);
        player.play();
        System.err.println(String.format("Solved in in : %f ms", (System.nanoTime()-solveStartTime)/1000000.0));
    }
}
