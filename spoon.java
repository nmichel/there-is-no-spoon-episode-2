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

    private final Graph graph;
    private final boolean[] activeNodes;

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

    public void play()  {
    }

    public static void main(String args[]) {
        final Graph graph = Graph.buildGraphFromInput();
        final Player player = new Player(graph);
        player.play();

        // Write an action using System.out.println()
        // To debug: System.err.println("Debug messages...");


        // Two coordinates and one integer: a node, one of its neighbors, the number of links connecting them.
        System.out.println("0 0 2 0 1");
    }
}