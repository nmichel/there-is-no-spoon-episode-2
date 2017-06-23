var TOP    = 0;
var LEFT   = 1;
var BOTTOM = 2;
var RIGHT  = 3;
var oppositeDirections = [BOTTOM, RIGHT, TOP, LEFT];

function getOppositeDirection(direction) {
    return oppositeDirections[direction];
}

var nextId = 1;

function Node(x, y, v) {
    this.id = nextId++;
    this.x = x;
    this.y = y;
    this.key = [x, y];
    this.target = v;
    this.current = 0;
    this.links = new Array(4).fill(null);
}
Node.prototype = Object.create(Object);
Node.prototype.constructor = Node;
Node.prototype.link = function(direction, other) {
    this.links[direction] = other;
    other.links[getOppositeDirection(direction)] = this;
};
Node.prototype.unlink = function(direction) {
    var other = this.links[direction]; 
    this.links[direction] = null;
    other.links[getOppositeDirection(direction)] = null;
};
Node.prototype.getKey = function() {
    return this.key;
};
Node.prototype.toString = function() {
    return '' + this.id;
/*    
    return '' + this.target + this.links.reduce(function(acc, val) {
        return acc + (val === null ? '' : ('[' + val.x + ' | '+ val.y + '] '));
    }, '');
*/
};

function buildGraphFromInput() {
    var width = parseInt(readline());
    var height = parseInt(readline());
    var markersW = new Array(width).fill(null);
    var graph = new Map();
    var total = 0;
    var nodes = [];

    for (var i = 0; i < height; ++i) {
        var markerH = null;
        var line = readline();
        for (var j = 0; j < width; ++j) {
            var c = line[j];
            if (c === '.') {
                continue; // <== 
            }
            var value = parseInt(c);
            total += value;
            var node = new Node(i, j, value);
            nodes.push(node);
            graph.set(node.getKey(), node);
            if (markerH !== null) {
                markerH.link(RIGHT, node);
            }
            markerH = node;
            if (markersW[j] !== null) {
                markersW[j].link(BOTTOM, node);
            }
            markersW[j] = node;
        }
    }
    return [total, nodes, graph];
}

var buildGraph = new Date();
var [total, nodes, graph] = buildGraphFromInput();
printErr("GRAPH", (new Date()) - buildGraph);

function getChoicesFromNode(node) {
    var r =
        node.links
            .map((v,i) => {return {from: node, to: v, dir: i}})
            .filter(c => c.v !== null)
            .sort((a, b) => a.target < b.target);
    return r;
}

function buildChoices(nodes) {
    for (var [k, v] of graph.entries()) {
        v.choices = getChoicesFromNode(v);
    }
}

var buildChoicesDate = new Date();
buildChoices(nodes);
printErr("CHOICES", (new Date()) - buildChoicesDate);

var stack = [];

function isNodeInStack(node) {
    printErr('stack size', stack.length);
    return stack.find( ({node: n}) => n === node ) !== undefined;
}

function dumpStack(stack) {
    var res = new Map();
    for (var i = 1; i < stack.length; ++i) {
        var {from: {x: fromX, y: fromY}, to: {x: toX, y: toY}} = stack[i].op;
        var minX = Math.min(fromX, toX);
        var maxX = Math.max(fromX, toX);
        var minY = Math.min(fromY, toY);
        var maxY = Math.max(fromY, toY);
        var k = [minX, minY, maxX, maxY];
        if (res.has(k)) {
            res.set(k, 2);
        }
        else {
            res.set(k, 1);
        }
    }
    for (var [[minX, minY, maxX, maxY], v] of res.entries()) {
        print('' + minY + ' ' + minX + ' ' + maxY + ' ' + maxX + ' ' + v);
    }
}

function crossH(operation, stack) {
    for (var i = 1; i < stack.length; ++i) {
        var op = stack[i].op;
        if (op.from.y !== op.to.y) {
            continue; // <==  not an horizontal
        }
        var refY = op.from.y;
        var minY = Math.min(operation.from.y, operation.to.y);
        var maxY = Math.max(operation.from.y, operation.to.y);
        if (refY <= minY || refY >= maxY) {
            continue; // <== 
        }
        
        var refX = operation.from.x;
        var minX = Math.min(op.from.x, op.to.x);
        var maxX = Math.max(op.from.x, op.to.x);
        if (refX <= minX || refX >= maxX) {
            continue; // <== 
        }

        return true; // <== found one edge crossing
    }
    return false;
}

function crossV(operation, stack) {
    for (var i = 1; i < stack.length; ++i) {
        var op = stack[i].op;
        if (op.from.x !== op.to.x) {
            continue; // <==  not an horizontal
        }
        var refX = op.from.x;
        var minX = Math.min(operation.from.x, operation.to.x);
        var maxX = Math.max(operation.from.x, operation.to.x);
        if (refX <= minX || refX >= maxX) {
            continue; // <== 
        }
        
        var refY = operation.from.y;
        var minY = Math.min(op.from.y, op.to.y);
        var maxY = Math.max(op.from.y, op.to.y);
        if (refY <= minY || refY >= maxY) {
            continue; // <== 
        }
        
        return true; // <== found one edge crossing
    }
    return false;
}

function cross(operation, stack) {
    var {from: from, to: to, dir: dir} = operation;
    if (dir === TOP || dir === BOTTOM) {
        return crossH(operation, stack);
    }
    return crossV(operation, stack);
}

function commitOperation(operation, stack) {
    var {from: from, to: to} = operation;

    if (to === null) {
        return false; // <== source node has no link along direction
    }

    if (from.current === from.target || to.current === to.target) {
        return false; // <== source and/or target node are "full"
    }
    if (cross(operation, stack)) {
        return false; // <== operation would cross a edge
    }

    ++from.current;
    ++to.current;
    
    return true;
}

function rollbackOperation({from: from, to: to, dir: dir}) {
    --from.current;
    --to.current;
}

var choices = new Array(31*31*4);

function copyChoices(from, pos) {
    var i = 0;
    for (; i < from.length; ++i) {
        choices[pos+i] = from[i];
    }
    return pos+i;
}

var sortedNodes = nodes.sort((a, b) => a.target > b.target);

for (var i = 0; i < sortedNodes.length; ++i) {
    var root = sortedNodes[i];
    var stopPos = copyChoices(root.choices, 0);
    stack.push({op: null, node: root, startPos: 0, stopPos: stopPos});
    while(total > 0 && stack.length > 0) {
        var ctxt = stack[stack.length-1];

        if (ctxt.startPos === ctxt.stopPos) {
            if (ctxt.op !== null) {
                rollbackOperation(ctxt.op);
                total += 2;
            }
            stack.pop();
            continue; // <== 
        }
        
        var choice = choices[ctxt.startPos++];
        if (!commitOperation(choice, stack)) {
            continue; // <== 
        }
        
        var target = choice.to;
        var newCtxt = {};
        newCtxt.op = choice;
        newCtxt.node = isNodeInStack(target) ? null : target;
        newCtxt.startPos = ctxt.startPos;
        newCtxt.stopPos = newCtxt.node !== null ? copyChoices(target.choices, ctxt.stopPos) : ctxt.stopPos;
        total -= 2;

        stack.push(newCtxt);
    }
    if (total === 0) {
        dumpStack(stack);
        break; // <== 
    }
    stack.pop();
}
