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

    node.select("circle")
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    node.select("text")
        .attr("dy", "0.25em")
        .attr("dx", "-0.65em")
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " + d.split.operand.toFixed(2);
            }
            if (d.hoeffdingBound && d.className) {
                return d.className;
            }
            else if (d.className) {
                return d.className;
            }
            else if (d.hoeffdingBound) {
                return d.hoeffdingBound;
            }
            return "unknown";
        });

    // Update links
    link.exit().remove();

    svg.transition()
        .duration(duration)
        .selectAll(".link")
        .attr("d", diagonal)
        .style("stroke-width", function (d) {
            return d.target.instancesSeen;
        })
        .call(endall, function () {
            log.debug('Update animations finished.');
            animationsFinished = true;
        });
}

function updateTreeNodeLabels() {
    // Get node and link values.
    let nodes = tree.nodes(root).reverse();

    let node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id;
        });

    node.select("circle")
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    node.select("text")
        .attr("dy", "0.25em")
        .attr("dx", "-0.65em")
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " + d.split.operand.toFixed(2);
            }
            if (d.hoeffdingBound && d.className) {
                return d.className;
            }
            else if (d.className) {
                return d.className;
            }
            else if (d.hoeffdingBound) {
                return d.hoeffdingBound;
            }
            return "unknown";
        });
}

function renderNodes(node) {
    let nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.parent.px + "," + d.parent.py + ")";
        })
        .attr("id", function (d) {
            return d.id;
        });

    // Add entering nodes in the parent’s old position.
    nodeEnter.append("svg:circle")
        .attr("r", function (d) {
            return d.r | 8;
        })
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    nodeEnter.append("svg:text")
        .attr("dy", "0.25em")
        .attr("dx", "-0.65em")
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            if (d.split) {
                return d.split.attribute + " " + d.split.operator + " " + d.split.operand.toFixed(2);
            }
            if (d.hoeffdingBound && d.className) {
                return d.className;
            }
            else if (d.className) {
                return d.className;
            }
            else if (d.hoeffdingBound) {
                return d.hoeffdingBound;
            }
            return "unknown";
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
            let o = {
                x: d.source.px,
                y: d.source.py
            };
            return diagonal({source: o, target: o});
        });

    // Transition nodes and links to their new positions.
    svg.transition()
        .duration(duration)
        .selectAll(".link")
        .attr("id", function (d) {
            return d.target.id;
        })
        .attr("d", diagonal)
        .style("stroke-width", function (d) {
            return d.target.instancesSeen;
        })
        .call(endall, function () {
            log.debug('Enter animations finished.');
            animationsFinished = true;
        });
}