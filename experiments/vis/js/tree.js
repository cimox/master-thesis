function printTrainingTree(chart, timeout, currentTree, previousTree) {
    (function () {
        setTimeout(function () {
            root = currentTree;
            root.x0 = height / 2;
            root.y0 = 0;
            update(root);
        }, timeout);
    })();
}

function training(chart, timeout) {
    $.getJSON("data/tree-training_mini.json", function (trees) {
        console.log('Training...');
        var previousTree = undefined, currentTree = undefined;

        for (var i = 0; i < trees.length; i++) {
            currentTree = _.cloneDeep(trees[i]);
            printTrainingTree(chart, timeout * i, currentTree, previousTree);
            previousTree = _.cloneDeep(trees[i]);
        }

        console.log('Done! Read ' + trees.length + ' instances');
    });
}

function enterNodeText(nodeUpdate) {
    // Add node information text
    nodeUpdate.append("svg:text")
        // .attr("dx", function (d) {
        //     return d.x + 15;
        // })
        // .attr("dy", function (d) {
        //     return d.y + 5;
        // })
        .text(function (d) {
            if (d.leaf) return d.className + ' | ' + d.weights;
            return "WOW";
        });
}

function updateNodeText(node) {
    console.log('Updating node text...');
    node.select("text")
        .attr("dx", function (d) {
            return d.x + 15;
        })
        .attr("dy", function (d) {
            return d.y + 5;
        })
        .text(function (d) {
            return "WOW";
            // if (d.leaf) return d.className + ' | ' + d.weights;
            // return d.className | d.id;
        });
}

function renderLabels(nodeEnter, nodeUpdate, nodeExit) {
    nodeEnter.append("svg:text")
        .attr("x", function (d) {
            return d.children || d._children ? -10 : 10;
        })
        .attr("dy", ".45em")
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .text(function (d) {
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " +
                    d.split.operand.toFixed(2);
            }
            return d.className;
        })
        .style("fill-opacity", 1e-6);

    nodeUpdate.select("text")
        .style("fill-opacity", 1);

    nodeExit.select("text")
        .style("fill-opacity", 1e-6);
}