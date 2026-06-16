(function () {
    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function safeArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function number(value) {
        var parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    function maxValue(items) {
        var highest = safeArray(items).reduce(function (current, item) {
            return Math.max(current, number(item && item.value));
        }, 0);
        return highest <= 0 ? 100 : highest;
    }

    function createEmptyState(container, message) {
        if (!container) {
            return;
        }

        container.innerHTML = [
            '<div class="analytics-empty">',
            '<h3>No data yet</h3>',
            "<p>" + escapeHtml(message || "Analytics will appear here once SmartATS collects enough signal.") + "</p>",
            "</div>"
        ].join("");
    }

    function createSkeleton(container) {
        if (!container) {
            return;
        }

        container.innerHTML = [
            '<div class="analytics-skeleton" aria-hidden="true">',
            '<span></span><span></span><span></span>',
            "</div>"
        ].join("");
    }

    function renderLineChart(container, items, options) {
        var data = safeArray(items).filter(function (item) {
            return item && item.label;
        });

        if (!container) {
            return;
        }

        if (!data.length) {
            createEmptyState(container, options && options.emptyMessage);
            return;
        }

        var width = 520;
        var height = 240;
        var paddingX = 34;
        var paddingTop = 18;
        var paddingBottom = 42;
        var chartWidth = width - (paddingX * 2);
        var chartHeight = height - paddingTop - paddingBottom;
        var highest = maxValue(data);
        var step = data.length > 1 ? chartWidth / (data.length - 1) : 0;

        var points = data.map(function (item, index) {
            var x = paddingX + (step * index);
            var y = paddingTop + chartHeight - ((number(item.value) / highest) * chartHeight);
            return { x: x, y: y, label: item.label, value: number(item.value) };
        });

        var linePath = points.map(function (point, index) {
            return (index === 0 ? "M" : "L") + point.x.toFixed(2) + " " + point.y.toFixed(2);
        }).join(" ");
        var areaPath = linePath + " L " + points[points.length - 1].x + " " + (paddingTop + chartHeight) + " L " + points[0].x + " " + (paddingTop + chartHeight) + " Z";

        container.innerHTML = [
            '<div class="analytics-chart-shell">',
            '<svg viewBox="0 0 ' + width + " " + height + '" class="analytics-chart-svg" role="img" aria-label="' + escapeHtml((options && options.title) || "Line chart") + '">',
            [0, 0.25, 0.5, 0.75, 1].map(function (ratio) {
                var y = paddingTop + chartHeight - (chartHeight * ratio);
                return '<line x1="' + paddingX + '" y1="' + y.toFixed(2) + '" x2="' + (width - paddingX) + '" y2="' + y.toFixed(2) + '" class="analytics-grid-line"></line>';
            }).join(""),
            '<path d="' + areaPath + '" class="analytics-area-path"></path>',
            '<path d="' + linePath + '" class="analytics-line-path"></path>',
            points.map(function (point) {
                return '<circle cx="' + point.x.toFixed(2) + '" cy="' + point.y.toFixed(2) + '" r="4.5" class="analytics-line-dot"></circle>';
            }).join(""),
            data.map(function (item, index) {
                var x = paddingX + (step * index);
                return '<text x="' + x.toFixed(2) + '" y="' + (height - 16) + '" class="analytics-axis-label" text-anchor="middle">' + escapeHtml(item.label) + "</text>";
            }).join(""),
            "</svg>",
            '<div class="analytics-legend-row">',
            data.slice(-3).map(function (item) {
                return '<span class="analytics-legend-pill"><strong>' + escapeHtml(item.label) + "</strong>" + escapeHtml(String(number(item.value))) + "</span>";
            }).join(""),
            "</div>",
            "</div>"
        ].join("");
    }

    function renderBarChart(container, items, options) {
        var data = safeArray(items).filter(function (item) {
            return item && item.label;
        });

        if (!container) {
            return;
        }

        if (!data.length) {
            createEmptyState(container, options && options.emptyMessage);
            return;
        }

        var highest = maxValue(data);
        container.innerHTML = [
            '<div class="analytics-bar-list">',
            data.map(function (item) {
                var value = number(item.value);
                var width = Math.max(6, Math.round((value / highest) * 100));
                return [
                    '<div class="analytics-bar-row">',
                    '<div class="analytics-bar-copy">',
                    '<strong>' + escapeHtml(item.label) + "</strong>",
                    item.meta ? '<span>' + escapeHtml(item.meta) + "</span>" : "<span>Tracked metric</span>",
                    "</div>",
                    '<div class="analytics-bar-track">',
                    '<div class="analytics-bar-fill" style="width:' + width + '%"></div>',
                    "</div>",
                    '<span class="analytics-bar-value">' + escapeHtml(String(value)) + "</span>",
                    "</div>"
                ].join("");
            }).join(""),
            "</div>"
        ].join("");
    }

    function renderDonutChart(container, items, options) {
        var data = safeArray(items).filter(function (item) {
            return number(item && item.value) > 0;
        });

        if (!container) {
            return;
        }

        if (!data.length) {
            createEmptyState(container, options && options.emptyMessage);
            return;
        }

        var total = data.reduce(function (sum, item) {
            return sum + number(item.value);
        }, 0);
        var stops = [];
        var start = 0;
        var palette = {
            success: "#1f7a4d",
            warning: "#b47a1a",
            danger: "#bf3434",
            primary: "#bf5a36",
            neutral: "#7d7f92"
        };

        data.forEach(function (item) {
            var end = start + ((number(item.value) / total) * 100);
            stops.push((palette[item.tone] || palette.primary) + " " + start.toFixed(2) + "% " + end.toFixed(2) + "%");
            start = end;
        });

        container.innerHTML = [
            '<div class="analytics-donut-layout">',
            '<div class="analytics-donut-ring" style="background:conic-gradient(' + stops.join(",") + ')">',
            '<div class="analytics-donut-center">',
            '<strong>' + escapeHtml(String(total)) + "</strong>",
            '<span>' + escapeHtml((options && options.centerLabel) || "Total") + "</span>",
            "</div>",
            "</div>",
            '<div class="analytics-donut-legend">',
            data.map(function (item) {
                return [
                    '<div class="analytics-donut-item">',
                    '<span class="analytics-donut-swatch" style="background:' + (palette[item.tone] || palette.primary) + '"></span>',
                    '<div class="analytics-donut-copy">',
                    '<strong>' + escapeHtml(item.label) + "</strong>",
                    '<span>' + escapeHtml(String(item.percentage || 0)) + "% | " + escapeHtml(String(number(item.value))) + "</span>",
                    "</div>",
                    "</div>"
                ].join("");
            }).join(""),
            "</div>",
            "</div>"
        ].join("");
    }

    function renderRadarChart(container, items, options) {
        var data = safeArray(items).filter(function (item) {
            return item && item.label;
        });

        if (!container) {
            return;
        }

        if (data.length < 3) {
            createEmptyState(container, options && options.emptyMessage);
            return;
        }

        var width = 320;
        var height = 320;
        var centerX = width / 2;
        var centerY = height / 2;
        var radius = 110;
        var levels = [0.25, 0.5, 0.75, 1];
        var angleStep = (Math.PI * 2) / data.length;

        function polarPoint(value, index, scale) {
            var angle = (-Math.PI / 2) + (angleStep * index);
            var valueRadius = radius * scale * (number(value) / 100);
            return {
                x: centerX + Math.cos(angle) * valueRadius,
                y: centerY + Math.sin(angle) * valueRadius
            };
        }

        function polygonPoints(scale) {
            return data.map(function (_, index) {
                var angle = (-Math.PI / 2) + (angleStep * index);
                return (centerX + Math.cos(angle) * radius * scale).toFixed(2) + "," + (centerY + Math.sin(angle) * radius * scale).toFixed(2);
            }).join(" ");
        }

        var valuePoints = data.map(function (item, index) {
            var point = polarPoint(item.value, index, 1);
            return point.x.toFixed(2) + "," + point.y.toFixed(2);
        }).join(" ");

        container.innerHTML = [
            '<div class="analytics-chart-shell">',
            '<svg viewBox="0 0 ' + width + " " + height + '" class="analytics-radar-svg" role="img" aria-label="' + escapeHtml((options && options.title) || "Radar chart") + '">',
            levels.map(function (level) {
                return '<polygon points="' + polygonPoints(level) + '" class="analytics-radar-grid"></polygon>';
            }).join(""),
            data.map(function (_, index) {
                var angle = (-Math.PI / 2) + (angleStep * index);
                return '<line x1="' + centerX + '" y1="' + centerY + '" x2="' + (centerX + Math.cos(angle) * radius).toFixed(2) + '" y2="' + (centerY + Math.sin(angle) * radius).toFixed(2) + '" class="analytics-radar-axis"></line>';
            }).join(""),
            '<polygon points="' + valuePoints + '" class="analytics-radar-shape"></polygon>',
            data.map(function (item, index) {
                var point = polarPoint(item.value, index, 1);
                var labelPoint = polarPoint(100, index, 1.12);
                return [
                    '<circle cx="' + point.x.toFixed(2) + '" cy="' + point.y.toFixed(2) + '" r="4.5" class="analytics-radar-dot"></circle>',
                    '<text x="' + labelPoint.x.toFixed(2) + '" y="' + labelPoint.y.toFixed(2) + '" class="analytics-radar-label" text-anchor="middle">' + escapeHtml(item.label) + "</text>"
                ].join("");
            }).join(""),
            "</svg>",
            "</div>"
        ].join("");
    }

    function renderHeatmap(container, items, options) {
        var data = safeArray(items).filter(function (item) {
            return item && item.label;
        });

        if (!container) {
            return;
        }

        if (!data.length) {
            createEmptyState(container, options && options.emptyMessage);
            return;
        }

        container.innerHTML = [
            '<div class="analytics-heatmap-grid">',
            data.map(function (item) {
                var value = number(item.value);
                var intensity = Math.max(0.18, value / 100);
                return [
                    '<div class="analytics-heatmap-cell" style="background:rgba(191,90,54,' + intensity.toFixed(2) + ')">',
                    '<span>' + escapeHtml(item.label) + "</span>",
                    '<strong>' + escapeHtml(String(value)) + "%</strong>",
                    item.meta ? "<small>" + escapeHtml(item.meta) + "</small>" : "<small>Tracked</small>",
                    "</div>"
                ].join("");
            }).join(""),
            "</div>"
        ].join("");
    }

    window.analyticsCharts = {
        escapeHtml: escapeHtml,
        createEmptyState: createEmptyState,
        createSkeleton: createSkeleton,
        renderLineChart: renderLineChart,
        renderBarChart: renderBarChart,
        renderDonutChart: renderDonutChart,
        renderRadarChart: renderRadarChart,
        renderHeatmap: renderHeatmap
    };
})();
