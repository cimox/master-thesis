function updateTree() {
    // Get node and link values.
    var nodes = tree.nodes(root).reverse(),
        links = tree.links(nodes);

    var node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id;
        });
    var link = svg.selectAll(".link")
        .data(links, function (d) {
            return d.source.id + "-" + d.target.id;
        });

    // Update nodes
    node.exit().remove()
        .transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x; d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });

    node.transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x; d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });

    node.select("text")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) return "root";
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " +
                    d.split.operand.toFixed(2);
            }
            return d.weights;
        });

    // Update links
    link.exit().remove();

    svg.transition()
        .duration(duration)
        .selectAll(".link")
        .attr("d", diagonal);
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