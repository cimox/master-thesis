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

function renderLabels(node) {
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.parent.px + "," + d.parent.py + ")";
        });

    // Add entering nodes in the parent’s old position.
    nodeEnter.append("svg:circle")
        .attr("r", 6.5)
        .style("fill", function (d) {
            return d.leaf ? "red" : "blue";
        });

    nodeEnter.append("svg:text")
        .attr("dy", "0.25em")
        .attr("dx", "-0.65em")
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .style("font-color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) return "root";
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " +
                    d.split.operand.toFixed(2);
            }
            return d.weights;
        });

    // Transition to the proper position for the node
    node.transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x; d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });
}

function renderLinks(link, svg) {
    // Add entering links in the parent’s old position.
    link.enter().insert("path", "g")
        .attr("class", "link")
        .attr("d", function (d) {
            var o = {
                x: d.source.px,
                y: d.source.py
            };
            return diagonal({source: o, target: o});
        });

    // Transition nodes and links to their new positions.
    svg.transition()
        .duration(duration)
        .selectAll(".link")
        .attr("d", diagonal);
}