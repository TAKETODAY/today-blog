var XRegExp;
if (XRegExp) {
    throw Error("can't load XRegExp twice in the same frame")
} (function(d) {
    XRegExp = function(w, r) {
        var q = [],
        u = XRegExp.OUTSIDE_CLASS,
        x = 0,
        p,
        s,
        v,
        t,
        y;
        if (XRegExp.isRegExp(w)) {
            if (r !== d) {
                throw TypeError("can't supply flags when constructing one RegExp from another")
            }
            return j(w)
        }
        if (g) {
            throw Error("can't call the XRegExp constructor within token definition functions")
        }
        r = r || "";
        p = {
            hasNamedCapture: false,
            captureNames: [],
            hasFlag: function(z) {
                return r.indexOf(z) > -1
            },
            setFlag: function(z) {
                r += z
            }
        };
        while (x < w.length) {
            s = o(w, x, u, p);
            if (s) {
                q.push(s.output);
                x += (s.match[0].length || 1)
            } else {
                if (v = l.exec.call(i[u], w.slice(x))) {
                    q.push(v[0]);
                    x += v[0].length
                } else {
                    t = w.charAt(x);
                    if (t === "[") {
                        u = XRegExp.INSIDE_CLASS
                    } else {
                        if (t === "]") {
                            u = XRegExp.OUTSIDE_CLASS
                        }
                    }
                    q.push(t);
                    x++
                }
            }
        }
        y = RegExp(q.join(""), l.replace.call(r, h, ""));
        y._xregexp = {
            source: w,
            captureNames: p.hasNamedCapture ? p.captureNames: null
        };
        return y
    };
    XRegExp.version = "1.5.1";
    XRegExp.INSIDE_CLASS = 1;
    XRegExp.OUTSIDE_CLASS = 2;
    var c = /\$(?:(\d\d?|[$&`'])|{([$\w]+)})/g,
    h = /[^gimy]+|([\s\S])(?=[\s\S]*\1)/g,
    n = /^(?:[?*+]|{\d+(?:,\d*)?})\??/,
    g = false,
    k = [],
    l = {
        exec: RegExp.prototype.exec,
        test: RegExp.prototype.test,
        match: String.prototype.match,
        replace: String.prototype.replace,
        split: String.prototype.split
    },
    a = l.exec.call(/()??/, "")[1] === d,
    f = function() {
        var p = /^/g;
        l.test.call(p, "");
        return ! p.lastIndex
    } (),
    b = RegExp.prototype.sticky !== d,
    i = {};
    i[XRegExp.INSIDE_CLASS] = /^(?:\\(?:[0-3][0-7]{0,2}|[4-7][0-7]?|x[\dA-Fa-f]{2}|u[\dA-Fa-f]{4}|c[A-Za-z]|[\s\S]))/;
    i[XRegExp.OUTSIDE_CLASS] = /^(?:\\(?:0(?:[0-3][0-7]{0,2}|[4-7][0-7]?)?|[1-9]\d*|x[\dA-Fa-f]{2}|u[\dA-Fa-f]{4}|c[A-Za-z]|[\s\S])|\(\?[:=!]|[?*+]\?|{\d+(?:,\d*)?}\??)/;
    XRegExp.addToken = function(s, r, q, p) {
        k.push({
            pattern: j(s, "g" + (b ? "y": "")),
            handler: r,
            scope: q || XRegExp.OUTSIDE_CLASS,
            trigger: p || null
        })
    };
    XRegExp.cache = function(r, p) {
        var q = r + "/" + (p || "");
        return XRegExp.cache[q] || (XRegExp.cache[q] = XRegExp(r, p))
    };
    XRegExp.copyAsGlobal = function(p) {
        return j(p, "g")
    };
    XRegExp.escape = function(p) {
        return p.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&")
    };
    XRegExp.execAt = function(t, s, u, r) {
        var p = j(s, "g" + ((r && b) ? "y": "")),
        q;
        p.lastIndex = u = u || 0;
        q = p.exec(t);
        if (r && q && q.index !== u) {
            q = null
        }
        if (s.global) {
            s.lastIndex = q ? p.lastIndex: 0
        }
        return q
    };
    XRegExp.freezeTokens = function() {
        XRegExp.addToken = function() {
            throw Error("can't run addToken after freezeTokens")
        }
    };
    XRegExp.isRegExp = function(p) {
        return Object.prototype.toString.call(p) === "[object RegExp]"
    };
    XRegExp.iterate = function(u, t, v, s) {
        var p = j(t, "g"),
        r = -1,
        q;
        while (q = p.exec(u)) {
            if (t.global) {
                t.lastIndex = p.lastIndex
            }
            v.call(s, q, ++r, u, t);
            if (p.lastIndex === q.index) {
                p.lastIndex++
            }
        }
        if (t.global) {
            t.lastIndex = 0
        }
    };
    XRegExp.matchChain = function(q, p) {
        return function r(s, x) {
            var v = p[x].regex ? p[x] : {
                regex: p[x]
            },
            u = j(v.regex, "g"),
            w = [],
            t;
            for (t = 0; t < s.length; t++) {
                XRegExp.iterate(s[t], u,
                function(y) {
                    w.push(v.backref ? (y[v.backref] || "") : y[0])
                })
            }
            return ((x === p.length - 1) || !w.length) ? w: r(w, x + 1)
        } ([q], 0)
    };
    RegExp.prototype.apply = function(q, p) {
        return this.exec(p[0])
    };
    RegExp.prototype.call = function(p, q) {
        return this.exec(q)
    };
    RegExp.prototype.exec = function(u) {
        var s, r, q, p;
        if (!this.global) {
            p = this.lastIndex
        }
        s = l.exec.apply(this, arguments);
        if (s) {
            if (!a && s.length > 1 && m(s, "") > -1) {
                q = RegExp(this.source, l.replace.call(e(this), "g", ""));
                l.replace.call((u + "").slice(s.index), q,
                function() {
                    for (var v = 1; v < arguments.length - 2; v++) {
                        if (arguments[v] === d) {
                            s[v] = d
                        }
                    }
                })
            }
            if (this._xregexp && this._xregexp.captureNames) {
                for (var t = 1; t < s.length; t++) {
                    r = this._xregexp.captureNames[t - 1];
                    if (r) {
                        s[r] = s[t]
                    }
                }
            }
            if (!f && this.global && !s[0].length && (this.lastIndex > s.index)) {
                this.lastIndex--
            }
        }
        if (!this.global) {
            this.lastIndex = p
        }
        return s
    };
    RegExp.prototype.test = function(r) {
        var q, p;
        if (!this.global) {
            p = this.lastIndex
        }
        q = l.exec.call(this, r);
        if (q && !f && this.global && !q[0].length && (this.lastIndex > q.index)) {
            this.lastIndex--
        }
        if (!this.global) {
            this.lastIndex = p
        }
        return !! q
    };
    String.prototype.match = function(q) {
        if (!XRegExp.isRegExp(q)) {
            q = RegExp(q)
        }
        if (q.global) {
            var p = l.match.apply(this, arguments);
            q.lastIndex = 0;
            return p
        }
        return q.exec(this)
    };
    String.prototype.replace = function(s, t) {
        var u = XRegExp.isRegExp(s),
        r,
        q,
        v,
        p;
        if (u) {
            if (s._xregexp) {
                r = s._xregexp.captureNames
            }
            if (!s.global) {
                p = s.lastIndex
            }
        } else {
            s = s + ""
        }
        if (Object.prototype.toString.call(t) === "[object Function]") {
            q = l.replace.call(this + "", s,
            function() {
                if (r) {
                    arguments[0] = new String(arguments[0]);
                    for (var w = 0; w < r.length; w++) {
                        if (r[w]) {
                            arguments[0][r[w]] = arguments[w + 1]
                        }
                    }
                }
                if (u && s.global) {
                    s.lastIndex = arguments[arguments.length - 2] + arguments[0].length
                }
                return t.apply(null, arguments)
            })
        } else {
            v = this + "";
            q = l.replace.call(v, s,
            function() {
                var w = arguments;
                return l.replace.call(t + "", c,
                function(y, x, B) {
                    if (x) {
                        switch (x) {
                        case "$":
                            return "$";
                        case "&":
                            return w[0];
                        case "`":
                            return w[w.length - 1].slice(0, w[w.length - 2]);
                        case "'":
                            return w[w.length - 1].slice(w[w.length - 2] + w[0].length);
                        default:
                            var z = "";
                            x = +x;
                            if (!x) {
                                return y
                            }
                            while (x > w.length - 3) {
                                z = String.prototype.slice.call(x, -1) + z;
                                x = Math.floor(x / 10)
                            }
                            return (x ? w[x] || "": "$") + z
                        }
                    } else {
                        var A = +B;
                        if (A <= w.length - 3) {
                            return w[A]
                        }
                        A = r ? m(r, B) : -1;
                        return A > -1 ? w[A + 1] : y
                    }
                })
            })
        }
        if (u) {
            if (s.global) {
                s.lastIndex = 0
            } else {
                s.lastIndex = p
            }
        }
        return q
    };
    String.prototype.split = function(u, p) {
        if (!XRegExp.isRegExp(u)) {
            return l.split.apply(this, arguments)
        }
        var w = this + "",
        r = [],
        v = 0,
        t,
        q;
        if (p === d || +p < 0) {
            p = Infinity
        } else {
            p = Math.floor( + p);
            if (!p) {
                return []
            }
        }
        u = XRegExp.copyAsGlobal(u);
        while (t = u.exec(w)) {
            if (u.lastIndex > v) {
                r.push(w.slice(v, t.index));
                if (t.length > 1 && t.index < w.length) {
                    Array.prototype.push.apply(r, t.slice(1))
                }
                q = t[0].length;
                v = u.lastIndex;
                if (r.length >= p) {
                    break
                }
            }
            if (u.lastIndex === t.index) {
                u.lastIndex++
            }
        }
        if (v === w.length) {
            if (!l.test.call(u, "") || q) {
                r.push("")
            }
        } else {
            r.push(w.slice(v))
        }
        return r.length > p ? r.slice(0, p) : r
    };
    function j(r, q) {
        if (!XRegExp.isRegExp(r)) {
            throw TypeError("type RegExp expected")
        }
        var p = r._xregexp;
        r = XRegExp(r.source, e(r) + (q || ""));
        if (p) {
            r._xregexp = {
                source: p.source,
                captureNames: p.captureNames ? p.captureNames.slice(0) : null
            }
        }
        return r
    }
    function e(p) {
        return (p.global ? "g": "") + (p.ignoreCase ? "i": "") + (p.multiline ? "m": "") + (p.extended ? "x": "") + (p.sticky ? "y": "")
    }
    function o(v, u, w, p) {
        var r = k.length,
        y, s, x;
        g = true;
        try {
            while (r--) {
                x = k[r];
                if ((w & x.scope) && (!x.trigger || x.trigger.call(p))) {
                    x.pattern.lastIndex = u;
                    s = x.pattern.exec(v);
                    if (s && s.index === u) {
                        y = {
                            output: x.handler.call(p, s, w),
                            match: s
                        };
                        break
                    }
                }
            }
        } catch(q) {
            throw q
        } finally {
            g = false
        }
        return y
    }
    function m(s, q, r) {
        if (Array.prototype.indexOf) {
            return s.indexOf(q, r)
        }
        for (var p = r || 0; p < s.length; p++) {
            if (s[p] === q) {
                return p
            }
        }
        return - 1
    }
    XRegExp.addToken(/\(\?#[^)]*\)/,
    function(p) {
        return l.test.call(n, p.input.slice(p.index + p[0].length)) ? "": "(?:)"
    });
    XRegExp.addToken(/\((?!\?)/,
    function() {
        this.captureNames.push(null);
        return "("
    });
    XRegExp.addToken(/\(\?<([$\w]+)>/,
    function(p) {
        this.captureNames.push(p[1]);
        this.hasNamedCapture = true;
        return "("
    });
    XRegExp.addToken(/\\k<([\w$]+)>/,
    function(q) {
        var p = m(this.captureNames, q[1]);
        return p > -1 ? "\\" + (p + 1) + (isNaN(q.input.charAt(q.index + q[0].length)) ? "": "(?:)") : q[0]
    });
    XRegExp.addToken(/\[\^?]/,
    function(p) {
        return p[0] === "[]" ? "\\b\\B": "[\\s\\S]"
    });
    XRegExp.addToken(/^\(\?([imsx]+)\)/,
    function(p) {
        this.setFlag(p[1]);
        return ""
    });
    XRegExp.addToken(/(?:\s+|#.*)+/,
    function(p) {
        return l.test.call(n, p.input.slice(p.index + p[0].length)) ? "": "(?:)"
    },
    XRegExp.OUTSIDE_CLASS,
    function() {
        return this.hasFlag("x")
    });
    XRegExp.addToken(/\./,
    function() {
        return "[\\s\\S]"
    },
    XRegExp.OUTSIDE_CLASS,
    function() {
        return this.hasFlag("s")
    })
})();
if (typeof(SyntaxHighlighter) == "undefined") {
    var SyntaxHighlighter = function() {
        if (typeof(require) != "undefined" && typeof(XRegExp) == "undefined") {
            XRegExp = require("XRegExp").XRegExp
        }
        var l = {
            defaults: {
                "class-name": "",
                "first-line": 1,
                "pad-line-numbers": false,
                "highlight": true,
                "title": null,
                "smart-tabs": true,
                "tab-size": 4,
                "gutter": true,
                "toolbar": true,
                "quick-code": true,
                "collapse": false,
                "auto-links": false,
                "light": false,
                "unindent": true,
                "html-script": false
            },
            config: {
                space: "&nbsp;",
                useScriptTags: true,
                bloggerMode: false,
                stripBrs: false,
                tagName: "pre",
                strings: {
                    expandSource: "expand source",
                    help: "?",
                    alert: "SyntaxHighlighter\n\n",
                    noBrush: "Can't find brush for: ",
                    brushNotHtmlScript: "Brush wasn't configured for html-script option: ",
                    aboutDialog: "@ABOUT@"
                }
            },
            vars: {
                discoveredBrushes: null,
                highlighters: {}
            },
            brushes: {},
            regexLib: {
                multiLineCComments: /\/\*[\s\S]*?\*\//gm,
                singleLineCComments: /\/\/.*$/gm,
                singleLinePerlComments: /#.*$/gm,
                doubleQuotedString: /"([^\\"\n]|\\.)*"/g,
                singleQuotedString: /'([^\\'\n]|\\.)*'/g,
                multiLineDoubleQuotedString: new XRegExp('"([^\\\\"]|\\\\.)*"', "gs"),
                multiLineSingleQuotedString: new XRegExp("'([^\\\\']|\\\\.)*'", "gs"),
                xmlComments: /(&lt;|<)!--[\s\S]*?--(&gt;|>)/gm,
                url: /\w+:\/\/[\w-.\/?%&=:@;#]*/g,
                phpScriptTags: {
                    left: /(&lt;|<)\?(?:=|php)?/g,
                    right: /\?(&gt;|>)/g,
                    "eof": true
                },
                aspScriptTags: {
                    left: /(&lt;|<)%=?/g,
                    right: /%(&gt;|>)/g
                },
                scriptScriptTags: {
                    left: /(&lt;|<)\s*script.*?(&gt;|>)/gi,
                    right: /(&lt;|<)\/\s*script\s*(&gt;|>)/gi
                }
            },
            toolbar: {
                getHtml: function(L) {
                    var N = '<div class="toolbar">',
                    K = l.toolbar.items,
                    P = K.list;
                    function O(R, Q) {
                        return l.toolbar.getButtonHtml(R, Q, l.config.strings[Q])
                    }
                    for (var M = 0; M < P.length; M++) {
                        N += (K[P[M]].getHtml || O)(L, P[M])
                    }
                    N += "</div>";
                    return N
                },
                getButtonHtml: function(L, M, K) {
                    return '<span><a href="#" class="toolbar_item' + " command_" + M + " " + M + '">' + K + "</a></span>"
                },
                handler: function(P) {
                    var O = P.target,
                    N = O.className || "";
                    function K(R) {
                        var S = new RegExp(R + "_(\\w+)"),
                        Q = S.exec(N);
                        return Q ? Q[1] : null
                    }
                    var L = s(B(O, ".syntaxhighlighter").id),
                    M = K("command");
                    if (L && M) {
                        l.toolbar.items[M].execute(L)
                    }
                    P.preventDefault()
                },
                items: {
                    list: ["expandSource", "help"],
                    expandSource: {
                        getHtml: function(K) {
                            if (K.getParam("collapse") != true) {
                                return ""
                            }
                            var L = K.getParam("title");
                            return l.toolbar.getButtonHtml(K, "expandSource", L ? L: l.config.strings.expandSource)
                        },
                        execute: function(K) {
                            var L = I(K.id);
                            j(L, "collapsed")
                        }
                    },
                    help: {
                        execute: function(K) {
                            var L = w("", "_blank", 500, 250, "scrollbars=0"),
                            M = L.document;
                            M.write(l.config.strings.aboutDialog);
                            M.close();
                            L.focus()
                        }
                    }
                }
            },
            findElements: function(O, N) {
                var Q = N ? [N] : r(document.getElementsByTagName(l.config.tagName)),
                L = l.config,
                K = [];
                if (L.useScriptTags) {
                    Q = Q.concat(J())
                }
                if (Q.length === 0) {
                    return K
                }
                for (var M = 0; M < Q.length; M++) {
                    var P = {
                        target: Q[M],
                        params: A(O, o(Q[M].className))
                    };
                    if (P.params["brush"] == null) {
                        continue
                    }
                    K.push(P)
                }
                return K
            },
            highlight: function(Q, O) {
                var K = this.findElements(Q, O),
                R = "innerHTML",
                V = null,
                T = l.config;
                if (K.length === 0) {
                    return
                }
                for (var P = 0; P < K.length; P++) {
                    var O = K[P],
                    S = O.target,
                    M = O.params,
                    W = M.brush,
                    L;
                    if (W == null) {
                        continue
                    }
                    if (M["html-script"] == "true" || l.defaults["html-script"] == true) {
                        V = new l.HtmlScript(W);
                        W = "htmlscript"
                    } else {
                        var U = g(W);
                        if (U) {
                            V = new U()
                        } else {
                            continue
                        }
                    }
                    L = S[R];
                    if (T.useScriptTags) {
                        L = C(L)
                    }
                    if ((S.title || "") != "") {
                        M.title = S.title
                    }
                    M["brush"] = W;
                    V.init(M);
                    O = V.getDiv(L);
                    if ((S.id || "") != "") {
                        O.id = S.id
                    }
                    var N = O.firstChild.firstChild;
                    N.className = O.firstChild.className;
                    S.parentNode.replaceChild(N, S)
                }
            },
            all: function(K) {
                f(window, "load",
                function() {
                    l.highlight(K)
                })
            }
        };
        function D(L, K) {
            return L.className.indexOf(K) != -1
        }
        function t(L, K) {
            if (!D(L, K)) {
                L.className += " " + K
            }
        }
        function j(L, K) {
            L.className = L.className.replace(K, "")
        }
        function r(M) {
            var K = [];
            for (var L = 0; L < M.length; L++) {
                K.push(M[L])
            }
            return K
        }
        function v(K) {
            return K.split(/\r?\n/)
        }
        function E(L) {
            var K = "highlighter_";
            return L.indexOf(K) == 0 ? L: K + L
        }
        function s(K) {
            return l.vars.highlighters[E(K)]
        }
        function I(K) {
            return document.getElementById(E(K))
        }
        function n(K) {
            l.vars.highlighters[E(K.id)] = K
        }
        function h(R, O, M) {
            if (R == null) {
                return null
            }
            var L = M != true ? R.childNodes: [R.parentNode],
            P = {
                "#": "id",
                ".": "className"
            } [O.substr(0, 1)] || "nodeName",
            K,
            Q;
            K = P != "nodeName" ? O.substr(1) : O.toUpperCase();
            if ((R[P] || "").indexOf(K) != -1) {
                return R
            }
            for (var N = 0; L && N < L.length && Q == null; N++) {
                Q = h(L[N], O, M)
            }
            return Q
        }
        function B(L, K) {
            return h(L, K, true)
        }
        function k(N, K, M) {
            M = Math.max(M || 0, 0);
            for (var L = M; L < N.length; L++) {
                if (N[L] == K) {
                    return L
                }
            }
            return - 1
        }
        function p(K) {
            return (K || "") + Math.round(Math.random() * 1000000).toString()
        }
        function A(N, M) {
            var K = {},
            L;
            for (L in N) {
                K[L] = N[L]
            }
            for (L in M) {
                K[L] = M[L]
            }
            return K
        }
        function d(L) {
            var K = {
                "true": true,
                "false": false
            } [L];
            return K == null ? L: K
        }
        function w(O, N, P, L, M) {
            var K = (screen.width - P) / 2,
            R = (screen.height - L) / 2;
            M += ", left=" + K + ", top=" + R + ", width=" + P + ", height=" + L;
            M = M.replace(/^,/, "");
            var Q = window.open(O, N, M);
            Q.focus();
            return Q
        }
        function f(O, M, N, L) {
            function K(P) {
                P = P || window.event;
                if (!P.target) {
                    P.target = P.srcElement;
                    P.preventDefault = function() {
                        this.returnValue = false
                    }
                }
                N.call(L || window, P)
            }
            if (O.attachEvent) {
                O.attachEvent("on" + M, K)
            } else {
                O.addEventListener(M, K, false)
            }
        }
        function y(K) {
            window.alert(l.config.strings.alert + K)
        }
        function g(O, Q) {
            var P = l.vars.discoveredBrushes,
            K = null;
            if (P == null) {
                P = {};
                for (var M in l.brushes) {
                    var R = l.brushes[M],
                    L = R.aliases;
                    if (L == null) {
                        continue
                    }
                    R.brushName = M.toLowerCase();
                    for (var N = 0; N < L.length; N++) {
                        P[L[N]] = M
                    }
                }
                l.vars.discoveredBrushes = P
            }
            K = l.brushes[P[O]];
            if (K == null && Q) {
                y(l.config.strings.noBrush + O)
            }
            return K
        }
        function H(M, N) {
            var K = v(M);
            for (var L = 0; L < K.length; L++) {
                K[L] = N(K[L], L)
            }
            return K.join("\r\n")
        }
        function G(K) {
            return K.replace(/^[ ]*[\n]+|[\n]*[ ]*$/g, "")
        }
        function o(Q) {
            var M, L = {},
            N = new XRegExp("^\\[(?<values>(.*?))\\]$"),
            O = new XRegExp("(?<name>[\\w-]+)" + "\\s*:\\s*" + "(?<value>" + "[\\w-%#]+|" + "\\[.*?\\]|" + '".*?"|' + "'.*?'" + ")\\s*;?", "g");
            while ((M = O.exec(Q)) != null) {
                var P = M.value.replace(/^['"]|['"]$/g, "");
                if (P != null && N.test(P)) {
                    var K = N.exec(P);
                    P = K.values.length > 0 ? K.values.split(/\s*,\s*/) : []
                }
                L[M.name] = P
            }
            return L
        }
        function x(L, K) {
            if (L == null || L.length == 0 || L == "\n") {
                return L
            }
            L = L.replace(/</g, "&lt;");
            L = L.replace(/ {2,}/g,
            function(M) {
                var N = "";
                for (var O = 0; O < M.length - 1; O++) {
                    N += l.config.space
                }
                return N + " "
            });
            if (K != null) {
                L = H(L,
                function(M) {
                    if (M.length == 0) {
                        return ""
                    }
                    var N = "";
                    M = M.replace(/^(&nbsp;| )+/,
                    function(O) {
                        N = O;
                        return ""
                    });
                    if (M.length == 0) {
                        return N
                    }
                    return N + '<code class="' + K + '">' + M + "</code>"
                })
            }
            return L
        }
        function c(M, L) {
            var K = M.toString();
            while (K.length < L) {
                K = "0" + K
            }
            return K
        }
        function F(M, N) {
            var L = "";
            for (var K = 0; K < N; K++) {
                L += " "
            }
            return M.replace(/\t/g, L)
        }
        function u(O, P) {
            var K = v(O),
            N = "\t",
            L = "";
            for (var M = 0; M < 50; M++) {
                L += "                    "
            }
            function Q(R, T, S) {
                return R.substr(0, T) + L.substr(0, S) + R.substr(T + 1, R.length)
            }
            O = H(O,
            function(R) {
                if (R.indexOf(N) == -1) {
                    return R
                }
                var T = 0;
                while ((T = R.indexOf(N)) != -1) {
                    var S = P - T % P;
                    R = Q(R, T, S)
                }
                return R
            });
            return O
        }
        function i(L) {
            var K = /<br\s*\/?>|&lt;br\s*\/?&gt;/gi;
            if (l.config.bloggerMode == true) {
                L = L.replace(K, "\n")
            }
            if (l.config.stripBrs == true) {
                L = L.replace(K, "")
            }
            return L
        }
        function a(K) {
            return K.replace(/^\s+|\s+$/g, "")
        }
        function z(R) {
            var L = v(i(R)),
            Q = new Array(),
            O = /^\s*/,
            N = 1000;
            for (var M = 0; M < L.length && N > 0; M++) {
                var K = L[M];
                if (a(K).length == 0) {
                    continue
                }
                var P = O.exec(K);
                if (P == null) {
                    return R
                }
                N = Math.min(P[0].length, N)
            }
            if (N > 0) {
                for (var M = 0; M < L.length; M++) {
                    L[M] = L[M].substr(N)
                }
            }
            return L.join("\n")
        }
        function m(L, K) {
            if (L.index < K.index) {
                return - 1
            } else {
                if (L.index > K.index) {
                    return 1
                } else {
                    if (L.length < K.length) {
                        return - 1
                    } else {
                        if (L.length > K.length) {
                            return 1
                        }
                    }
                }
            }
            return 0
        }
        function q(O, Q) {
            function R(S, T) {
                return S[0]
            }
            var M = 0,
            L = null,
            P = [],
            N = Q.func ? Q.func: R;
            while ((L = Q.regex.exec(O)) != null) {
                var K = N(L, Q);
                if (typeof(K) == "string") {
                    K = [new l.Match(K, L.index, Q.css)]
                }
                P = P.concat(K)
            }
            return P
        }
        function b(L) {
            var K = /(.*)((&gt;|&lt;).*)/;
            return L.replace(l.regexLib.url,
            function(M) {
                var O = "",
                N = null;
                if (N = K.exec(M)) {
                    M = N[1];
                    O = N[2]
                }
                return '<a href="' + M + '">' + M + "</a>" + O
            })
        }
        function J() {
            var L = document.getElementsByTagName("script"),
            K = [];
            for (var M = 0; M < L.length; M++) {
                if (L[M].type == "syntaxhighlighter") {
                    K.push(L[M])
                }
            }
            return K
        }
        function C(N) {
            var P = "<![CDATA[",
            M = "]]>",
            R = a(N),
            Q = false,
            L = P.length,
            O = M.length;
            if (R.indexOf(P) == 0) {
                R = R.substring(L);
                Q = true
            }
            var K = R.length;
            if (R.indexOf(M) == K - O) {
                R = R.substring(0, K - O);
                Q = true
            }
            return Q ? R: N
        }
        function e(O) {
            var P = O.target,
            N = B(P, ".syntaxhighlighter"),
            K = B(P, ".container"),
            R = document.createElement("textarea"),
            Q;
            if (!K || !N || h(K, "textarea")) {
                return
            }
            Q = s(N.id);
            t(N, "source");
            var S = K.childNodes,
            L = [];
            for (var M = 0; M < S.length; M++) {
                L.push(S[M].innerText || S[M].textContent)
            }
            L = L.join("\r");
            L = L.replace(/\u00a0/g, " ");
            R.appendChild(document.createTextNode(L));
            K.appendChild(R);
            R.focus();
            R.select();
            f(R, "blur",
            function(T) {
                R.parentNode.removeChild(R);
                j(N, "source")
            })
        }
        l.Match = function(M, K, L) {
            this.value = M;
            this.index = K;
            this.length = M.length;
            this.css = L;
            this.brushName = null
        };
        l.Match.prototype.toString = function() {
            return this.value
        };
        l.HtmlScript = function(O) {
            var S = g(O),
            R,
            K = new l.brushes.Xml(),
            Q = null,
            M = this,
            N = "getDiv getHtml init".split(" ");
            if (S == null) {
                return
            }
            R = new S();
            for (var P = 0; P < N.length; P++) { (function() {
                    var U = N[P];
                    M[U] = function() {
                        return K[U].apply(K, arguments)
                    }
                })()
            }
            if (R.htmlScript == null) {
                y(l.config.strings.brushNotHtmlScript + O);
                return
            }
            K.regexList.push({
                regex: R.htmlScript.code,
                func: L
            });
            function T(V, W) {
                for (var U = 0; U < V.length; U++) {
                    V[U].index += W
                }
            }
            function L(ab, V) {
                var U = ab.code,
                aa = [],
                Z = R.regexList,
                X = ab.index + ab.left.length,
                ac = R.htmlScript,
                ad;
                for (var Y = 0; Y < Z.length; Y++) {
                    ad = q(U, Z[Y]);
                    T(ad, X);
                    aa = aa.concat(ad)
                }
                if (ac.left != null && ab.left != null) {
                    ad = q(ab.left, ac.left);
                    T(ad, ab.index);
                    aa = aa.concat(ad)
                }
                if (ac.right != null && ab.right != null) {
                    ad = q(ab.right, ac.right);
                    T(ad, ab.index + ab[0].lastIndexOf(ab.right));
                    aa = aa.concat(ad)
                }
                for (var W = 0; W < aa.length; W++) {
                    aa[W].brushName = S.brushName
                }
                return aa
            }
        };
        l.Highlighter = function() {};
        l.Highlighter.prototype = {
            getParam: function(M, L) {
                var K = this.params[M];
                return d(K == null ? L: K)
            },
            create: function(K) {
                return document.createElement(K)
            },
            findMatches: function(N, M) {
                var K = [];
                if (N != null) {
                    for (var L = 0; L < N.length; L++) {
                        if (typeof(N[L]) == "object") {
                            K = K.concat(q(M, N[L]))
                        }
                    }
                }
                return this.removeNestedMatches(K.sort(m))
            },
            removeNestedMatches: function(O) {
                for (var N = 0; N < O.length; N++) {
                    if (O[N] === null) {
                        continue
                    }
                    var K = O[N],
                    M = K.index + K.length;
                    for (var L = N + 1; L < O.length && O[N] !== null; L++) {
                        var P = O[L];
                        if (P === null) {
                            continue
                        } else {
                            if (P.index > M) {
                                break
                            } else {
                                if (P.index == K.index && P.length > K.length) {
                                    O[N] = null
                                } else {
                                    if (P.index >= K.index && P.index < M) {
                                        O[L] = null
                                    }
                                }
                            }
                        }
                    }
                }
                return O
            },
            figureOutLineNumbers: function(M) {
                var K = [],
                L = parseInt(this.getParam("first-line"));
                H(M,
                function(N, O) {
                    K.push(O + L)
                });
                return K
            },
            isLineHighlighted: function(K) {
                var L = this.getParam("highlight", []);
                if (typeof(L) != "object" && L.push == null) {
                    L = [L]
                }
                return k(L, K.toString()) != -1
            },
            getLineHtml: function(N, K, M) {
                var L = ["line", "number" + K, "index" + N, "alt" + (K % 2 == 0 ? 1 : 2).toString()];
                if (this.isLineHighlighted(K)) {
                    L.push("highlighted")
                }
                if (K == 0) {
                    L.push("break")
                }
                return '<div class="' + L.join(" ") + '">' + M + "</div>"
            },
            getLineNumbersHtml: function(Q, L) {
                var O = "",
                P = v(Q).length,
                M = parseInt(this.getParam("first-line")),
                R = this.getParam("pad-line-numbers");
                if (R == true) {
                    R = (M + P - 1).toString().length
                } else {
                    if (isNaN(R) == true) {
                        R = 0
                    }
                }
                for (var N = 0; N < P; N++) {
                    var K = L ? L[N] : M + N,
                    Q = K == 0 ? l.config.space: c(K, R);
                    O += this.getLineHtml(N, K, Q)
                }
                return O
            },
            getCodeLinesHtml: function(N, R) {
                N = a(N);
                var T = v(N),
                O = this.getParam("pad-line-numbers"),
                Q = parseInt(this.getParam("first-line")),
                N = "",
                S = this.getParam("brush");
                for (var M = 0; M < T.length; M++) {
                    var U = T[M],
                    K = /^(&nbsp;|\s)+/.exec(U),
                    P = null,
                    L = R ? R[M] : Q + M;
                    if (K != null) {
                        P = K[0].toString();
                        U = U.substr(P.length);
                        P = P.replace(" ", l.config.space)
                    }
                    U = a(U);
                    if (U.length == 0) {
                        U = l.config.space
                    }
                    N += this.getLineHtml(M, L, (P != null ? '<code class="' + S + ' spaces">' + P + "</code>": "") + U)
                }
                return N
            },
            getTitleHtml: function(K) {
                return K ? "<caption>" + K + "</caption>": ""
            },
            getMatchesHtml: function(K, O) {
                var Q = 0,
                S = "",
                R = this.getParam("brush", "");
                function M(U) {
                    var T = U ? (U.brushName || R) : R;
                    return T ? T + " ": ""
                }
                for (var N = 0; N < O.length; N++) {
                    var P = O[N],
                    L;
                    if (P === null || P.length === 0) {
                        continue
                    }
                    L = M(P);
                    S += x(K.substr(Q, P.index - Q), L + "plain") + x(P.value, L + P.css);
                    Q = P.index + P.length + (P.offset || 0)
                }
                S += x(K.substr(Q), M() + "plain");
                return S
            },
            getHtml: function(N) {
                var M = "",
                L = ["syntaxhighlighter"],
                P,
                O,
                K;
                if (this.getParam("light") == true) {
                    this.params.toolbar = this.params.gutter = false
                }
                className = "syntaxhighlighter";
                if (this.getParam("collapse") == true) {
                    L.push("collapsed")
                }
                if ((gutter = this.getParam("gutter")) == false) {
                    L.push("nogutter")
                }
                L.push(this.getParam("class-name"));
                L.push(this.getParam("brush"));
                N = G(N).replace(/\r/g, " ");
                P = this.getParam("tab-size");
                N = this.getParam("smart-tabs") == true ? u(N, P) : F(N, P);
                if (this.getParam("unindent")) {
                    N = z(N)
                }
                if (gutter) {
                    K = this.figureOutLineNumbers(N)
                }
                O = this.findMatches(this.regexList, N);
                M = this.getMatchesHtml(N, O);
                M = this.getCodeLinesHtml(M, K);
                if (this.getParam("auto-links")) {
                    M = b(M)
                }
                if (typeof(navigator) != "undefined" && navigator.userAgent && navigator.userAgent.match(/MSIE/)) {
                    L.push("ie")
                }
                M = '<div id="' + E(this.id) + '" class="' + L.join(" ") + '">' + (this.getParam("toolbar") ? l.toolbar.getHtml(this) : "") + '<table border="0" cellpadding="0" cellspacing="0">' + this.getTitleHtml(this.getParam("title")) + "<tbody>" + "<tr>" + (gutter ? '<td class="gutter">' + this.getLineNumbersHtml(N) + "</td>": "") + '<td class="code">' + '<div class="container">' + M + "</div>" + "</td>" + "</tr>" + "</tbody>" + "</table>" + "</div>";
                return M
            },
            getDiv: function(K) {
                if (K === null) {
                    K = ""
                }
                this.code = K;
                var L = this.create("div");
                L.innerHTML = this.getHtml(K);
                if (this.getParam("toolbar")) {
                    f(h(L, ".toolbar"), "click", l.toolbar.handler)
                }
                if (this.getParam("quick-code")) {
                    f(h(L, ".code"), "dblclick", e)
                }
                return L
            },
            init: function(K) {
                this.id = p();
                n(this);
                this.params = A(l.defaults, K || {});
                if (this.getParam("light") == true) {
                    this.params.toolbar = this.params.gutter = false
                }
            },
            getKeywords: function(K) {
                K = K.replace(/^\s+|\s+$/g, "").replace(/\s+/g, "|");
                return "\\b(?:" + K + ")\\b"
            },
            forHtmlScript: function(K) {
                var L = {
                    "end": K.right.source
                };
                if (K.eof) {
                    L.end = "(?:(?:" + L.end + ")|$)"
                }
                this.htmlScript = {
                    left: {
                        regex: K.left,
                        css: "script"
                    },
                    right: {
                        regex: K.right,
                        css: "script"
                    },
                    code: new XRegExp("(?<left>" + K.left.source + ")" + "(?<code>.*?)" + "(?<right>" + L.end + ")", "sgi")
                }
            }
        };
        return l
    } ()
}
typeof(exports) != "undefined" ? exports.SyntaxHighlighter = SyntaxHighlighter: null; (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "class interface function package";
        var b = "-Infinity ...rest Array as AS3 Boolean break case catch const continue Date decodeURI " + "decodeURIComponent default delete do dynamic each else encodeURI encodeURIComponent escape " + "extends false final finally flash_proxy for get if implements import in include Infinity " + "instanceof int internal is isFinite isNaN isXMLName label namespace NaN native new null " + "Null Number Object object_proxy override parseFloat parseInt private protected public " + "return set static String super switch this throw true try typeof uint undefined unescape " + "use void while with";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\b([\d]+(\.[\d]+)?|0x[a-f0-9]+)\b/gi,
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "color3"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp("var", "gm"),
            css: "variable"
        },
        {
            regex: new RegExp("trace", "gm"),
            css: "color1"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.scriptScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["actionscript3", "as3"];
    SyntaxHighlighter.brushes.AS3 = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "after before beginning continue copy each end every from return get global in local named of set some that the then times to where whose with without";
        var d = "first second third fourth fifth sixth seventh eighth ninth tenth last front back middle";
        var b = "activate add alias AppleScript ask attachment boolean class constant delete duplicate empty exists false id integer list make message modal modified new no paragraph pi properties quit real record remove rest result reveal reverse run running save string true word yes";
        this.regexList = [{
            regex: /(--|#).*$/gm,
            css: "comments"
        },
        {
            regex: /\(\*(?:[\s\S]*?\(\*[\s\S]*?\*\))*[\s\S]*?\*\)/gm,
            css: "comments"
        },
        {
            regex: /"[\s\S]*?"/gm,
            css: "string"
        },
        {
            regex: /(?:,|:|??|'s\b|\(|\)|\{|\}|??|\b\w*??)/g,
            css: "color1"
        },
        {
            regex: /(-)?(\d)+(\.(\d)?)?(E\+(\d)+)?/g,
            css: "color1"
        },
        {
            regex: /(?:&(amp;|gt;|lt;)?|=|??? |>|<|???|>=|???|<=|\*|\+|-|\/|??|\^)/g,
            css: "color2"
        },
        {
            regex: /\b(?:and|as|div|mod|not|or|return(?!\s&)(ing)?|equals|(is(n't| not)? )?equal( to)?|does(n't| not) equal|(is(n't| not)? )?(greater|less) than( or equal( to)?)?|(comes|does(n't| not) come) (after|before)|is(n't| not)?( in)? (back|front) of|is(n't| not)? behind|is(n't| not)?( (in|contained by))?|does(n't| not) contain|contain(s)?|(start|begin|end)(s)? with|((but|end) )?(consider|ignor)ing|prop(erty)?|(a )?ref(erence)?( to)?|repeat (until|while|with)|((end|exit) )?repeat|((else|end) )?if|else|(end )?(script|tell|try)|(on )?error|(put )?into|(of )?(it|me)|its|my|with (timeout( of)?|transaction)|end (timeout|transaction))\b/g,
            css: "keyword"
        },
        {
            regex: /\b\d+(st|nd|rd|th)\b/g,
            css: "keyword"
        },
        {
            regex: /\b(?:about|above|against|around|at|below|beneath|beside|between|by|(apart|aside) from|(instead|out) of|into|on(to)?|over|since|thr(ough|u)|under)\b/g,
            css: "color3"
        },
        {
            regex: /\b(?:adding folder items to|after receiving|choose( ((remote )?application|color|folder|from list|URL))?|clipboard info|set the clipboard to|(the )?clipboard|entire contents|display(ing| (alert|dialog|mode))?|document( (edited|file|nib name))?|file( (name|type))?|(info )?for|giving up after|(name )?extension|quoted form|return(ed)?|second(?! item)(s)?|list (disks|folder)|text item(s| delimiters)?|(Unicode )?text|(disk )?item(s)?|((current|list) )?view|((container|key) )?window|with (data|icon( (caution|note|stop))?|parameter(s)?|prompt|properties|seed|title)|case|diacriticals|hyphens|numeric strings|punctuation|white space|folder creation|application(s( folder)?| (processes|scripts position|support))?|((desktop )?(pictures )?|(documents|downloads|favorites|home|keychain|library|movies|music|public|scripts|sites|system|users|utilities|workflows) )folder|desktop|Folder Action scripts|font(s| panel)?|help|internet plugins|modem scripts|(system )?preferences|printer descriptions|scripting (additions|components)|shared (documents|libraries)|startup (disk|items)|temporary items|trash|on server|in AppleTalk zone|((as|long|short) )?user name|user (ID|locale)|(with )?password|in (bundle( with identifier)?|directory)|(close|open for) access|read|write( permission)?|(g|s)et eof|using( delimiters)?|starting at|default (answer|button|color|country code|entr(y|ies)|identifiers|items|name|location|script editor)|hidden( answer)?|open(ed| (location|untitled))?|error (handling|reporting)|(do( shell)?|load|run|store) script|administrator privileges|altering line endings|get volume settings|(alert|boot|input|mount|output|set) volume|output muted|(fax|random )?number|round(ing)?|up|down|toward zero|to nearest|as taught in school|system (attribute|info)|((AppleScript( Studio)?|system) )?version|(home )?directory|(IPv4|primary Ethernet) address|CPU (type|speed)|physical memory|time (stamp|to GMT)|replacing|ASCII (character|number)|localized string|from table|offset|summarize|beep|delay|say|(empty|multiple) selections allowed|(of|preferred) type|invisibles|showing( package contents)?|editable URL|(File|FTP|News|Media|Web) [Ss]ervers|Telnet hosts|Directory services|Remote applications|waiting until completion|saving( (in|to))?|path (for|to( (((current|frontmost) )?application|resource))?)|POSIX (file|path)|(background|RGB) color|(OK|cancel) button name|cancel button|button(s)?|cubic ((centi)?met(re|er)s|yards|feet|inches)|square ((kilo)?met(re|er)s|miles|yards|feet)|(centi|kilo)?met(re|er)s|miles|yards|feet|inches|lit(re|er)s|gallons|quarts|(kilo)?grams|ounces|pounds|degrees (Celsius|Fahrenheit|Kelvin)|print( (dialog|settings))?|clos(e(able)?|ing)|(de)?miniaturized|miniaturizable|zoom(ed|able)|attribute run|action (method|property|title)|phone|email|((start|end)ing|home) page|((birth|creation|current|custom|modification) )?date|((((phonetic )?(first|last|middle))|computer|host|maiden|related) |nick)?name|aim|icq|jabber|msn|yahoo|address(es)?|save addressbook|should enable action|city|country( code)?|formatte(r|d address)|(palette )?label|state|street|zip|AIM [Hh]andle(s)?|my card|select(ion| all)?|unsaved|(alpha )?value|entr(y|ies)|group|(ICQ|Jabber|MSN) handle|person|people|company|department|icon image|job title|note|organization|suffix|vcard|url|copies|collating|pages (across|down)|request print time|target( printer)?|((GUI Scripting|Script menu) )?enabled|show Computer scripts|(de)?activated|awake from nib|became (key|main)|call method|of (class|object)|center|clicked toolbar item|closed|for document|exposed|(can )?hide|idle|keyboard (down|up)|event( (number|type))?|launch(ed)?|load (image|movie|nib|sound)|owner|log|mouse (down|dragged|entered|exited|moved|up)|move|column|localization|resource|script|register|drag (info|types)|resigned (active|key|main)|resiz(e(d)?|able)|right mouse (down|dragged|up)|scroll wheel|(at )?index|should (close|open( untitled)?|quit( after last window closed)?|zoom)|((proposed|screen) )?bounds|show(n)?|behind|in front of|size (mode|to fit)|update(d| toolbar item)?|was (hidden|miniaturized)|will (become active|close|finish launching|hide|miniaturize|move|open|quit|(resign )?active|((maximum|minimum|proposed) )?size|show|zoom)|bundle|data source|movie|pasteboard|sound|tool(bar| tip)|(color|open|save) panel|coordinate system|frontmost|main( (bundle|menu|window))?|((services|(excluded from )?windows) )?menu|((executable|frameworks|resource|scripts|shared (frameworks|support)) )?path|(selected item )?identifier|data|content(s| view)?|character(s)?|click count|(command|control|option|shift) key down|context|delta (x|y|z)|key( code)?|location|pressure|unmodified characters|types|(first )?responder|playing|(allowed|selectable) identifiers|allows customization|(auto saves )?configuration|visible|image( name)?|menu form representation|tag|user(-| )defaults|associated file name|(auto|needs) display|current field editor|floating|has (resize indicator|shadow)|hides when deactivated|level|minimized (image|title)|opaque|position|release when closed|sheet|title(d)?)\b/g,
            css: "color3"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "color3"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gm"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["applescript"];
    SyntaxHighlighter.brushes.AppleScript = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "if fi then elif else for do done until while break continue case esac function return in eq ne ge le";
        var b = "alias apropos awk basename bash bc bg builtin bzip2 cal cat cd cfdisk chgrp chmod chown chroot" + "cksum clear cmp comm command cp cron crontab csplit cut date dc dd ddrescue declare df " + "diff diff3 dig dir dircolors dirname dirs du echo egrep eject enable env ethtool eval " + "exec exit expand export expr false fdformat fdisk fg fgrep file find fmt fold format " + "free fsck ftp gawk getopts grep groups gzip hash head history hostname id ifconfig " + "import install join kill less let ln local locate logname logout look lpc lpr lprint " + "lprintd lprintq lprm ls lsof make man mkdir mkfifo mkisofs mknod more mount mtools " + "mv netstat nice nl nohup nslookup open op passwd paste pathchk ping popd pr printcap " + "printenv printf ps pushd pwd quota quotacheck quotactl ram rcp read readonly renice " + "remsync rm rmdir rsync screen scp sdiff sed select seq set sftp shift shopt shutdown " + "sleep sort source split ssh strace su sudo sum symlink sync tail tar tee test time " + "times touch top traceroute trap tr true tsort tty type ulimit umask umount unalias " + "uname unexpand uniq units unset unshar useradd usermod users uuencode uudecode v vdir " + "vi watch wc whereis which who whoami Wget xargs yes";
        this.regexList = [{
            regex: /^#!.*$/gm,
            css: "preprocessor bold"
        },
        {
            regex: /\/[\w-\/]+/gm,
            css: "plain"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleLinePerlComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "functions"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["bash", "shell", "sh"];
    SyntaxHighlighter.brushes.Bash = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "Abs ACos AddSOAPRequestHeader AddSOAPResponseHeader AjaxLink AjaxOnLoad ArrayAppend ArrayAvg ArrayClear ArrayDeleteAt " + "ArrayInsertAt ArrayIsDefined ArrayIsEmpty ArrayLen ArrayMax ArrayMin ArraySet ArraySort ArraySum ArraySwap ArrayToList " + "Asc ASin Atn BinaryDecode BinaryEncode BitAnd BitMaskClear BitMaskRead BitMaskSet BitNot BitOr BitSHLN BitSHRN BitXor " + "Ceiling CharsetDecode CharsetEncode Chr CJustify Compare CompareNoCase Cos CreateDate CreateDateTime CreateObject " + "CreateODBCDate CreateODBCDateTime CreateODBCTime CreateTime CreateTimeSpan CreateUUID DateAdd DateCompare DateConvert " + "DateDiff DateFormat DatePart Day DayOfWeek DayOfWeekAsString DayOfYear DaysInMonth DaysInYear DE DecimalFormat DecrementValue " + "Decrypt DecryptBinary DeleteClientVariable DeserializeJSON DirectoryExists DollarFormat DotNetToCFType Duplicate Encrypt " + "EncryptBinary Evaluate Exp ExpandPath FileClose FileCopy FileDelete FileExists FileIsEOF FileMove FileOpen FileRead " + "FileReadBinary FileReadLine FileSetAccessMode FileSetAttribute FileSetLastModified FileWrite Find FindNoCase FindOneOf " + "FirstDayOfMonth Fix FormatBaseN GenerateSecretKey GetAuthUser GetBaseTagData GetBaseTagList GetBaseTemplatePath " + "GetClientVariablesList GetComponentMetaData GetContextRoot GetCurrentTemplatePath GetDirectoryFromPath GetEncoding " + "GetException GetFileFromPath GetFileInfo GetFunctionList GetGatewayHelper GetHttpRequestData GetHttpTimeString " + "GetK2ServerDocCount GetK2ServerDocCountLimit GetLocale GetLocaleDisplayName GetLocalHostIP GetMetaData GetMetricData " + "GetPageContext GetPrinterInfo GetProfileSections GetProfileString GetReadableImageFormats GetSOAPRequest GetSOAPRequestHeader " + "GetSOAPResponse GetSOAPResponseHeader GetTempDirectory GetTempFile GetTemplatePath GetTickCount GetTimeZoneInfo GetToken " + "GetUserRoles GetWriteableImageFormats Hash Hour HTMLCodeFormat HTMLEditFormat IIf ImageAddBorder ImageBlur ImageClearRect " + "ImageCopy ImageCrop ImageDrawArc ImageDrawBeveledRect ImageDrawCubicCurve ImageDrawLine ImageDrawLines ImageDrawOval " + "ImageDrawPoint ImageDrawQuadraticCurve ImageDrawRect ImageDrawRoundRect ImageDrawText ImageFlip ImageGetBlob ImageGetBufferedImage " + "ImageGetEXIFTag ImageGetHeight ImageGetIPTCTag ImageGetWidth ImageGrayscale ImageInfo ImageNegative ImageNew ImageOverlay ImagePaste " + "ImageRead ImageReadBase64 ImageResize ImageRotate ImageRotateDrawingAxis ImageScaleToFit ImageSetAntialiasing ImageSetBackgroundColor " + "ImageSetDrawingColor ImageSetDrawingStroke ImageSetDrawingTransparency ImageSharpen ImageShear ImageShearDrawingAxis ImageTranslate " + "ImageTranslateDrawingAxis ImageWrite ImageWriteBase64 ImageXORDrawingMode IncrementValue InputBaseN Insert Int IsArray IsBinary " + "IsBoolean IsCustomFunction IsDate IsDDX IsDebugMode IsDefined IsImage IsImageFile IsInstanceOf IsJSON IsLeapYear IsLocalHost " + "IsNumeric IsNumericDate IsObject IsPDFFile IsPDFObject IsQuery IsSimpleValue IsSOAPRequest IsStruct IsUserInAnyRole IsUserInRole " + "IsUserLoggedIn IsValid IsWDDX IsXML IsXmlAttribute IsXmlDoc IsXmlElem IsXmlNode IsXmlRoot JavaCast JSStringFormat LCase Left Len " + "ListAppend ListChangeDelims ListContains ListContainsNoCase ListDeleteAt ListFind ListFindNoCase ListFirst ListGetAt ListInsertAt " + "ListLast ListLen ListPrepend ListQualify ListRest ListSetAt ListSort ListToArray ListValueCount ListValueCountNoCase LJustify Log " + "Log10 LSCurrencyFormat LSDateFormat LSEuroCurrencyFormat LSIsCurrency LSIsDate LSIsNumeric LSNumberFormat LSParseCurrency LSParseDateTime " + "LSParseEuroCurrency LSParseNumber LSTimeFormat LTrim Max Mid Min Minute Month MonthAsString Now NumberFormat ParagraphFormat ParseDateTime " + "Pi PrecisionEvaluate PreserveSingleQuotes Quarter QueryAddColumn QueryAddRow QueryConvertForGrid QueryNew QuerySetCell QuotedValueList Rand " + "Randomize RandRange REFind REFindNoCase ReleaseComObject REMatch REMatchNoCase RemoveChars RepeatString Replace ReplaceList ReplaceNoCase " + "REReplace REReplaceNoCase Reverse Right RJustify Round RTrim Second SendGatewayMessage SerializeJSON SetEncoding SetLocale SetProfileString " + "SetVariable Sgn Sin Sleep SpanExcluding SpanIncluding Sqr StripCR StructAppend StructClear StructCopy StructCount StructDelete StructFind " + "StructFindKey StructFindValue StructGet StructInsert StructIsEmpty StructKeyArray StructKeyExists StructKeyList StructKeyList StructNew " + "StructSort StructUpdate Tan TimeFormat ToBase64 ToBinary ToScript ToString Trim UCase URLDecode URLEncodedFormat URLSessionFormat Val " + "ValueList VerifyClient Week Wrap Wrap WriteOutput XmlChildPos XmlElemNew XmlFormat XmlGetNodeType XmlNew XmlParse XmlSearch XmlTransform " + "XmlValidate Year YesNoFormat";
        var d = "cfabort cfajaximport cfajaxproxy cfapplet cfapplication cfargument cfassociate cfbreak cfcache cfcalendar " + "cfcase cfcatch cfchart cfchartdata cfchartseries cfcol cfcollection cfcomponent cfcontent cfcookie cfdbinfo " + "cfdefaultcase cfdirectory cfdiv cfdocument cfdocumentitem cfdocumentsection cfdump cfelse cfelseif cferror " + "cfexchangecalendar cfexchangeconnection cfexchangecontact cfexchangefilter cfexchangemail cfexchangetask " + "cfexecute cfexit cffeed cffile cfflush cfform cfformgroup cfformitem cfftp cffunction cfgrid cfgridcolumn " + "cfgridrow cfgridupdate cfheader cfhtmlhead cfhttp cfhttpparam cfif cfimage cfimport cfinclude cfindex " + "cfinput cfinsert cfinterface cfinvoke cfinvokeargument cflayout cflayoutarea cfldap cflocation cflock cflog " + "cflogin cfloginuser cflogout cfloop cfmail cfmailparam cfmailpart cfmenu cfmenuitem cfmodule cfNTauthenticate " + "cfobject cfobjectcache cfoutput cfparam cfpdf cfpdfform cfpdfformparam cfpdfparam cfpdfsubform cfpod cfpop " + "cfpresentation cfpresentationslide cfpresenter cfprint cfprocessingdirective cfprocparam cfprocresult " + "cfproperty cfquery cfqueryparam cfregistry cfreport cfreportparam cfrethrow cfreturn cfsavecontent cfschedule " + "cfscript cfsearch cfselect cfset cfsetting cfsilent cfslider cfsprydataset cfstoredproc cfswitch cftable " + "cftextarea cfthread cfthrow cftimer cftooltip cftrace cftransaction cftree cftreeitem cftry cfupdate cfwddx " + "cfwindow cfxml cfzip cfzipparam";
        var b = "all and any between cross in join like not null or outer some";
        this.regexList = [{
            regex: new RegExp("--(.*)$", "gm"),
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.xmlComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gmi"),
            css: "functions"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gmi"),
            css: "color1"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gmi"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["coldfusion", "cf"];
    SyntaxHighlighter.brushes.ColdFusion = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var d = "ATOM BOOL BOOLEAN BYTE CHAR COLORREF DWORD DWORDLONG DWORD_PTR " + "DWORD32 DWORD64 FLOAT HACCEL HALF_PTR HANDLE HBITMAP HBRUSH " + "HCOLORSPACE HCONV HCONVLIST HCURSOR HDC HDDEDATA HDESK HDROP HDWP " + "HENHMETAFILE HFILE HFONT HGDIOBJ HGLOBAL HHOOK HICON HINSTANCE HKEY " + "HKL HLOCAL HMENU HMETAFILE HMODULE HMONITOR HPALETTE HPEN HRESULT " + "HRGN HRSRC HSZ HWINSTA HWND INT INT_PTR INT32 INT64 LANGID LCID LCTYPE " + "LGRPID LONG LONGLONG LONG_PTR LONG32 LONG64 LPARAM LPBOOL LPBYTE LPCOLORREF " + "LPCSTR LPCTSTR LPCVOID LPCWSTR LPDWORD LPHANDLE LPINT LPLONG LPSTR LPTSTR " + "LPVOID LPWORD LPWSTR LRESULT PBOOL PBOOLEAN PBYTE PCHAR PCSTR PCTSTR PCWSTR " + "PDWORDLONG PDWORD_PTR PDWORD32 PDWORD64 PFLOAT PHALF_PTR PHANDLE PHKEY PINT " + "PINT_PTR PINT32 PINT64 PLCID PLONG PLONGLONG PLONG_PTR PLONG32 PLONG64 POINTER_32 " + "POINTER_64 PSHORT PSIZE_T PSSIZE_T PSTR PTBYTE PTCHAR PTSTR PUCHAR PUHALF_PTR " + "PUINT PUINT_PTR PUINT32 PUINT64 PULONG PULONGLONG PULONG_PTR PULONG32 PULONG64 " + "PUSHORT PVOID PWCHAR PWORD PWSTR SC_HANDLE SC_LOCK SERVICE_STATUS_HANDLE SHORT " + "SIZE_T SSIZE_T TBYTE TCHAR UCHAR UHALF_PTR UINT UINT_PTR UINT32 UINT64 ULONG " + "ULONGLONG ULONG_PTR ULONG32 ULONG64 USHORT USN VOID WCHAR WORD WPARAM WPARAM WPARAM " + "char bool short int __int32 __int64 __int8 __int16 long float double __wchar_t " + "clock_t _complex _dev_t _diskfree_t div_t ldiv_t _exception _EXCEPTION_POINTERS " + "FILE _finddata_t _finddatai64_t _wfinddata_t _wfinddatai64_t __finddata64_t " + "__wfinddata64_t _FPIEEE_RECORD fpos_t _HEAPINFO _HFILE lconv intptr_t " + "jmp_buf mbstate_t _off_t _onexit_t _PNH ptrdiff_t _purecall_handler " + "sig_atomic_t size_t _stat __stat64 _stati64 terminate_function " + "time_t __time64_t _timeb __timeb64 tm uintptr_t _utimbuf " + "va_list wchar_t wctrans_t wctype_t wint_t signed";
        var b = "auto break case catch class const decltype __finally __exception __try " + "const_cast continue private public protected __declspec " + "default delete deprecated dllexport dllimport do dynamic_cast " + "else enum explicit extern if for friend goto inline " + "mutable naked namespace new noinline noreturn nothrow " + "register reinterpret_cast return selectany " + "sizeof static static_cast struct switch template this " + "thread throw true false try typedef typeid typename union " + "using uuid virtual void volatile whcar_t while";
        var c = "assert isalnum isalpha iscntrl isdigit isgraph islower isprint" + "ispunct isspace isupper isxdigit tolower toupper errno localeconv " + "setlocale acos asin atan atan2 ceil cos cosh exp fabs floor fmod " + "frexp ldexp log log10 modf pow sin sinh sqrt tan tanh jmp_buf " + "longjmp setjmp raise signal sig_atomic_t va_arg va_end va_start " + "clearerr fclose feof ferror fflush fgetc fgetpos fgets fopen " + "fprintf fputc fputs fread freopen fscanf fseek fsetpos ftell " + "fwrite getc getchar gets perror printf putc putchar puts remove " + "rename rewind scanf setbuf setvbuf sprintf sscanf tmpfile tmpnam " + "ungetc vfprintf vprintf vsprintf abort abs atexit atof atoi atol " + "bsearch calloc div exit free getenv labs ldiv malloc mblen mbstowcs " + "mbtowc qsort rand realloc srand strtod strtol strtoul system " + "wcstombs wctomb memchr memcmp memcpy memmove memset strcat strchr " + "strcmp strcoll strcpy strcspn strerror strlen strncat strncmp " + "strncpy strpbrk strrchr strspn strstr strtok strxfrm asctime " + "clock ctime difftime gmtime localtime mktime strftime time";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /^ *#.*/gm,
            css: "preprocessor"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gm"),
            css: "color1 bold"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "functions bold"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword bold"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["cpp", "c"];
    SyntaxHighlighter.brushes.Cpp = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "abstract as base bool break byte case catch char checked class const " + "continue decimal default delegate do double else enum event explicit volatile " + "extern false finally fixed float for foreach get goto if implicit in int " + "interface internal is lock long namespace new null object operator out " + "override params private protected public readonly ref return sbyte sealed set " + "short sizeof stackalloc static string struct switch this throw true try " + "typeof uint ulong unchecked unsafe ushort using virtual void while var " + "from group by into select let where orderby join on equals ascending descending";
        function b(d, f) {
            var e = (d[0].indexOf("///") == 0) ? "color1": "comments";
            return [new SyntaxHighlighter.Match(d[0], d.index, e)]
        }
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            func: b
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: /@"(?:[^"]|"")*"/g,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /^\s*#.*/gm,
            css: "preprocessor"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "keyword"
        },
        {
            regex: /\bpartial(?=\s+(?:class|interface|struct)\b)/g,
            css: "keyword"
        },
        {
            regex: /\byield(?=\s+(?:return|break)\b)/g,
            css: "keyword"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["c#", "c-sharp", "csharp"];
    SyntaxHighlighter.brushes.CSharp = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        function b(g) {
            return "\\b([a-z_]|)" + g.replace(/ /g, "(?=:)\\b|\\b([a-z_\\*]|\\*|)") + "(?=:)\\b"
        }
        function d(g) {
            return "\\b" + g.replace(/ /g, "(?!-)(?!:)\\b|\\b()") + ":\\b"
        }
        var e = "ascent azimuth background-attachment background-color background-image background-position " + "background-repeat background baseline bbox border-collapse border-color border-spacing border-style border-top " + "border-right border-bottom border-left border-top-color border-right-color border-bottom-color border-left-color " + "border-top-style border-right-style border-bottom-style border-left-style border-top-width border-right-width " + "border-bottom-width border-left-width border-width border bottom cap-height caption-side centerline clear clip color " + "content counter-increment counter-reset cue-after cue-before cue cursor definition-src descent direction display " + "elevation empty-cells float font-size-adjust font-family font-size font-stretch font-style font-variant font-weight font " + "height left letter-spacing line-height list-style-image list-style-position list-style-type list-style margin-top " + "margin-right margin-bottom margin-left margin marker-offset marks mathline max-height max-width min-height min-width orphans " + "outline-color outline-style outline-width outline overflow padding-top padding-right padding-bottom padding-left padding page " + "page-break-after page-break-before page-break-inside pause pause-after pause-before pitch pitch-range play-during position " + "quotes right richness size slope src speak-header speak-numeral speak-punctuation speak speech-rate stemh stemv stress " + "table-layout text-align top text-decoration text-indent text-shadow text-transform unicode-bidi unicode-range units-per-em " + "vertical-align visibility voice-family volume white-space widows width widths word-spacing x-height z-index";
        var c = "above absolute all always aqua armenian attr aural auto avoid baseline behind below bidi-override black blink block blue bold bolder " + "both bottom braille capitalize caption center center-left center-right circle close-quote code collapse compact condensed " + "continuous counter counters crop cross crosshair cursive dashed decimal decimal-leading-zero default digits disc dotted double " + "embed embossed e-resize expanded extra-condensed extra-expanded fantasy far-left far-right fast faster fixed format fuchsia " + "gray green groove handheld hebrew help hidden hide high higher icon inline-table inline inset inside invert italic " + "justify landscape large larger left-side left leftwards level lighter lime line-through list-item local loud lower-alpha " + "lowercase lower-greek lower-latin lower-roman lower low ltr marker maroon medium message-box middle mix move narrower " + "navy ne-resize no-close-quote none no-open-quote no-repeat normal nowrap n-resize nw-resize oblique olive once open-quote outset " + "outside overline pointer portrait pre print projection purple red relative repeat repeat-x repeat-y rgb ridge right right-side " + "rightwards rtl run-in screen scroll semi-condensed semi-expanded separate se-resize show silent silver slower slow " + "small small-caps small-caption smaller soft solid speech spell-out square s-resize static status-bar sub super sw-resize " + "table-caption table-cell table-column table-column-group table-footer-group table-header-group table-row table-row-group teal " + "text-bottom text-top thick thin top transparent tty tv ultra-condensed ultra-expanded underline upper-alpha uppercase upper-latin " + "upper-roman url visible wait white wider w-resize x-fast x-high x-large x-loud x-low x-slow x-small x-soft xx-large xx-small yellow";
        var f = "[mM]onospace [tT]ahoma [vV]erdana [aA]rial [hH]elvetica [sS]ans-serif [sS]erif [cC]ourier mono sans serif";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\#[a-fA-F0-9]{3,6}/g,
            css: "value"
        },
        {
            regex: /(-?\d+)(\.\d+)?(px|em|pt|\:|\%|)/g,
            css: "value"
        },
        {
            regex: /!important/g,
            css: "color3"
        },
        {
            regex: new RegExp(b(e), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(d(c), "g"),
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(f), "g"),
            css: "color1"
        }];
        this.forHtmlScript({
            left: /(&lt;|<)\s*style.*?(&gt;|>)/gi,
            right: /(&lt;|<)\/\s*style\s*(&gt;|>)/gi
        })
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["css"];
    SyntaxHighlighter.brushes.CSS = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "abs addr and ansichar ansistring array as asm begin boolean byte cardinal " + "case char class comp const constructor currency destructor div do double " + "downto else end except exports extended false file finalization finally " + "for function goto if implementation in inherited int64 initialization " + "integer interface is label library longint longword mod nil not object " + "of on or packed pansichar pansistring pchar pcurrency pdatetime pextended " + "pint64 pointer private procedure program property pshortstring pstring " + "pvariant pwidechar pwidestring protected public published raise real real48 " + "record repeat set shl shortint shortstring shr single smallint string then " + "threadvar to true try type unit until uses val var varirnt while widechar " + "widestring with word write writeln xor";
        this.regexList = [{
            regex: /\(\*[\s\S]*?\*\)/gm,
            css: "comments"
        },
        {
            regex: /{(?!\$)[\s\S]*?}/gm,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\{\$[a-zA-Z]+ .+\}/g,
            css: "color1"
        },
        {
            regex: /\b[\d\.]+\b/g,
            css: "value"
        },
        {
            regex: /\$[a-zA-Z0-9]+\b/g,
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gmi"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["delphi", "pascal", "pas"];
    SyntaxHighlighter.brushes.Delphi = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        this.regexList = [{
            regex: /^\+\+\+ .*$/gm,
            css: "color2"
        },
        {
            regex: /^\-\-\- .*$/gm,
            css: "color2"
        },
        {
            regex: /^\s.*$/gm,
            css: "color1"
        },
        {
            regex: /^@@.*@@.*$/gm,
            css: "variable"
        },
        {
            regex: /^\+.*$/gm,
            css: "string"
        },
        {
            regex: /^\-.*$/gm,
            css: "color3"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["diff", "patch"];
    SyntaxHighlighter.brushes.Diff = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "after and andalso band begin bnot bor bsl bsr bxor " + "case catch cond div end fun if let not of or orelse " + "query receive rem try when xor" + " module export import define";
        this.regexList = [{
            regex: new RegExp("[A-Z][A-Za-z0-9_]+", "g"),
            css: "constants"
        },
        {
            regex: new RegExp("\\%.+", "gm"),
            css: "comments"
        },
        {
            regex: new RegExp("\\?[A-Za-z0-9_]+", "g"),
            css: "preprocessor"
        },
        {
            regex: new RegExp("[a-z0-9_]+:[a-z0-9_]+", "g"),
            css: "functions"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["erl", "erlang"];
    SyntaxHighlighter.brushes.Erland = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var e = "as assert break case catch class continue def default do else extends finally " + "if in implements import instanceof interface new package property return switch " + "throw throws try while public protected private static";
        var d = "void boolean byte char short int long float double";
        var c = "null";
        var b = "allProperties count get size " + "collect each eachProperty eachPropertyName eachWithIndex find findAll " + "findIndexOf grep inject max min reverseEach sort " + "asImmutable asSynchronized flatten intersect join pop reverse subMap toList " + "padRight padLeft contains eachMatch toCharacter toLong toUrl tokenize " + "eachFile eachFileRecurse eachB yte eachLine readBytes readLine getText " + "splitEachLine withReader append encodeBase64 decodeBase64 filterLine " + "transformChar transformLine withOutputStream withPrintWriter withStream " + "withStreams withWriter withWriterAppend write writeLine " + "dump inspect invokeMethod print println step times upto use waitForOrKill " + "getText";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /""".*"""/g,
            css: "string"
        },
        {
            regex: new RegExp("\\b([\\d]+(\\.[\\d]+)?|0x[a-f0-9]+)\\b", "gi"),
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(e), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gm"),
            css: "color1"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "constants"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "functions"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["groovy"];
    SyntaxHighlighter.brushes.Groovy = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "abstract assert boolean break byte case catch char class const " + "continue default do double else enum extends " + "false final finally float for goto if implements import " + "instanceof int interface long native new null " + "package private protected public return " + "short static strictfp super switch synchronized this throw throws true " + "transient try void volatile while";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: /\/\*([^\*][\s\S]*)?\*\//gm,
            css: "comments"
        },
        {
            regex: /\/\*(?!\*\/)\*[\s\S]*?\*\//gm,
            css: "preprocessor"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\b([\d]+(\.[\d]+)?|0x[a-f0-9]+)\b/gi,
            css: "value"
        },
        {
            regex: /(?!\@interface\b)\@[\$\w]+\b/g,
            css: "color1"
        },
        {
            regex: /\@interface\b/g,
            css: "color2"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript({
            left: /(&lt;|<)%[@!=]?/g,
            right: /%(&gt;|>)/g
        })
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["java"];
    SyntaxHighlighter.brushes.Java = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "Boolean Byte Character Double Duration " + "Float Integer Long Number Short String Void";
        var b = "abstract after and as assert at before bind bound break catch class " + "continue def delete else exclusive extends false finally first for from " + "function if import in indexof init insert instanceof into inverse last " + "lazy mixin mod nativearray new not null on or override package postinit " + "protected public public-init public-read replace return reverse sizeof " + "step super then this throw true try tween typeof var where while with " + "attribute let private readonly static trigger";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: /(-?\.?)(\b(\d*\.?\d+|\d+\.?\d*)(e[+-]?\d+)?|0x[a-f\d]+)\b\.?/gi,
            css: "color2"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "variable"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["jfx", "javafx"];
    SyntaxHighlighter.brushes.JavaFX = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "break case catch continue " + "default delete do else false  " + "for function if in instanceof " + "new null return super switch " + "this throw true try typeof var while with";
        var c = SyntaxHighlighter.regexLib;
        this.regexList = [{
            regex: c.multiLineDoubleQuotedString,
            css: "string"
        },
        {
            regex: c.multiLineSingleQuotedString,
            css: "string"
        },
        {
            regex: c.singleLineCComments,
            css: "comments"
        },
        {
            regex: c.multiLineCComments,
            css: "comments"
        },
        {
            regex: /\s*#.*/gm,
            css: "preprocessor"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript(c.scriptScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["js", "jscript", "javascript"];
    SyntaxHighlighter.brushes.JScript = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "abs accept alarm atan2 bind binmode chdir chmod chomp chop chown chr " + "chroot close closedir connect cos crypt defined delete each endgrent " + "endhostent endnetent endprotoent endpwent endservent eof exec exists " + "exp fcntl fileno flock fork format formline getc getgrent getgrgid " + "getgrnam gethostbyaddr gethostbyname gethostent getlogin getnetbyaddr " + "getnetbyname getnetent getpeername getpgrp getppid getpriority " + "getprotobyname getprotobynumber getprotoent getpwent getpwnam getpwuid " + "getservbyname getservbyport getservent getsockname getsockopt glob " + "gmtime grep hex index int ioctl join keys kill lc lcfirst length link " + "listen localtime lock log lstat map mkdir msgctl msgget msgrcv msgsnd " + "oct open opendir ord pack pipe pop pos print printf prototype push " + "quotemeta rand read readdir readline readlink readpipe recv rename " + "reset reverse rewinddir rindex rmdir scalar seek seekdir select semctl " + "semget semop send setgrent sethostent setnetent setpgrp setpriority " + "setprotoent setpwent setservent setsockopt shift shmctl shmget shmread " + "shmwrite shutdown sin sleep socket socketpair sort splice split sprintf " + "sqrt srand stat study substr symlink syscall sysopen sysread sysseek " + "system syswrite tell telldir time times tr truncate uc ucfirst umask " + "undef unlink unpack unshift utime values vec wait waitpid warn write " + "say";
        var c = "bless caller continue dbmclose dbmopen die do dump else elsif eval exit " + "for foreach goto if import last local my next no our package redo ref " + "require return sub tie tied unless untie until use wantarray while " + "given when default " + "try catch finally " + "has extends with before after around override augment";
        this.regexList = [{
            regex: /(<<|&lt;&lt;)((\w+)|(['"])(.+?)\4)[\s\S]+?\n\3\5\n/g,
            css: "string"
        },
        {
            regex: /#.*$/gm,
            css: "comments"
        },
        {
            regex: /^#!.*\n/g,
            css: "preprocessor"
        },
        {
            regex: /-?\w+(?=\s*=(>|&gt;))/g,
            css: "string"
        },
        {
            regex: /\bq[qwxr]?\([\s\S]*?\)/g,
            css: "string"
        },
        {
            regex: /\bq[qwxr]?\{[\s\S]*?\}/g,
            css: "string"
        },
        {
            regex: /\bq[qwxr]?\[[\s\S]*?\]/g,
            css: "string"
        },
        {
            regex: /\bq[qwxr]?(<|&lt;)[\s\S]*?(>|&gt;)/g,
            css: "string"
        },
        {
            regex: /\bq[qwxr]?([^\w({<[])[\s\S]*?\1/g,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /(?:&amp;|[$@%*]|\$#)[a-zA-Z_](\w+|::)*/g,
            css: "variable"
        },
        {
            regex: /\b__(?:END|DATA)__\b[\s\S]*$/g,
            css: "comments"
        },
        {
            regex: /(^|\n)=\w[\s\S]*?(\n=cut\s*\n|$)/g,
            css: "comments"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "functions"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.phpScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["perl", "Perl", "pl"];
    SyntaxHighlighter.brushes.Perl = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "abs acos acosh addcslashes addslashes " + "array_change_key_case array_chunk array_combine array_count_values array_diff " + "array_diff_assoc array_diff_key array_diff_uassoc array_diff_ukey array_fill " + "array_filter array_flip array_intersect array_intersect_assoc array_intersect_key " + "array_intersect_uassoc array_intersect_ukey array_key_exists array_keys array_map " + "array_merge array_merge_recursive array_multisort array_pad array_pop array_product " + "array_push array_rand array_reduce array_reverse array_search array_shift " + "array_slice array_splice array_sum array_udiff array_udiff_assoc " + "array_udiff_uassoc array_uintersect array_uintersect_assoc " + "array_uintersect_uassoc array_unique array_unshift array_values array_walk " + "array_walk_recursive atan atan2 atanh base64_decode base64_encode base_convert " + "basename bcadd bccomp bcdiv bcmod bcmul bindec bindtextdomain bzclose bzcompress " + "bzdecompress bzerrno bzerror bzerrstr bzflush bzopen bzread bzwrite ceil chdir " + "checkdate checkdnsrr chgrp chmod chop chown chr chroot chunk_split class_exists " + "closedir closelog copy cos cosh count count_chars date decbin dechex decoct " + "deg2rad delete ebcdic2ascii echo empty end ereg ereg_replace eregi eregi_replace error_log " + "error_reporting escapeshellarg escapeshellcmd eval exec exit exp explode extension_loaded " + "feof fflush fgetc fgetcsv fgets fgetss file_exists file_get_contents file_put_contents " + "fileatime filectime filegroup fileinode filemtime fileowner fileperms filesize filetype " + "floatval flock floor flush fmod fnmatch fopen fpassthru fprintf fputcsv fputs fread fscanf " + "fseek fsockopen fstat ftell ftok getallheaders getcwd getdate getenv gethostbyaddr gethostbyname " + "gethostbynamel getimagesize getlastmod getmxrr getmygid getmyinode getmypid getmyuid getopt " + "getprotobyname getprotobynumber getrandmax getrusage getservbyname getservbyport gettext " + "gettimeofday gettype glob gmdate gmmktime ini_alter ini_get ini_get_all ini_restore ini_set " + "interface_exists intval ip2long is_a is_array is_bool is_callable is_dir is_double " + "is_executable is_file is_finite is_float is_infinite is_int is_integer is_link is_long " + "is_nan is_null is_numeric is_object is_readable is_real is_resource is_scalar is_soap_fault " + "is_string is_subclass_of is_uploaded_file is_writable is_writeable mkdir mktime nl2br " + "parse_ini_file parse_str parse_url passthru pathinfo print readlink realpath rewind rewinddir rmdir " + "round str_ireplace str_pad str_repeat str_replace str_rot13 str_shuffle str_split " + "str_word_count strcasecmp strchr strcmp strcoll strcspn strftime strip_tags stripcslashes " + "stripos stripslashes stristr strlen strnatcasecmp strnatcmp strncasecmp strncmp strpbrk " + "strpos strptime strrchr strrev strripos strrpos strspn strstr strtok strtolower strtotime " + "strtoupper strtr strval substr substr_compare";
        var d = "abstract and array as break case catch cfunction class clone const continue declare default die do " + "else elseif enddeclare endfor endforeach endif endswitch endwhile extends final for foreach " + "function global goto if implements include include_once interface instanceof insteadof namespace new " + "old_function or private protected public return require require_once static switch " + "trait throw try use var while xor ";
        var c = "__FILE__ __LINE__ __METHOD__ __FUNCTION__ __CLASS__";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\$\w+/g,
            css: "variable"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gmi"),
            css: "functions"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gmi"),
            css: "constants"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.phpScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["php"];
    SyntaxHighlighter.brushes.Php = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {}
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["text", "plain"];
    SyntaxHighlighter.brushes.Plain = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var d = "while validateset validaterange validatepattern validatelength validatecount " + "until trap switch return ref process param parameter in if global: " + "function foreach for finally filter end elseif else dynamicparam do default " + "continue cmdletbinding break begin alias \\? % #script #private #local #global " + "mandatory parametersetname position valuefrompipeline " + "valuefrompipelinebypropertyname valuefromremainingarguments helpmessage ";
        var b = " and as band bnot bor bxor casesensitive ccontains ceq cge cgt cle " + "clike clt cmatch cne cnotcontains cnotlike cnotmatch contains " + "creplace eq exact f file ge gt icontains ieq ige igt ile ilike ilt " + "imatch ine inotcontains inotlike inotmatch ireplace is isnot le like " + "lt match ne not notcontains notlike notmatch or regex replace wildcard";
        var e = "write where wait use update unregister undo trace test tee take suspend " + "stop start split sort skip show set send select scroll resume restore " + "restart resolve resize reset rename remove register receive read push " + "pop ping out new move measure limit join invoke import group get format " + "foreach export expand exit enter enable disconnect disable debug cxnew " + "copy convertto convertfrom convert connect complete compare clear " + "checkpoint aggregate add";
        var c = " component description example externalhelp forwardhelpcategory forwardhelptargetname forwardhelptargetname functionality inputs link notes outputs parameter remotehelprunspace role synopsis";
        this.regexList = [{
            regex: new RegExp("^\\s*#[#\\s]*\\.(" + this.getKeywords(c) + ").*$", "gim"),
            css: "preprocessor help bold"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleLinePerlComments,
            css: "comments"
        },
        {
            regex: /(&lt;|<)#[\s\S]*?#(&gt;|>)/gm,
            css: "comments here"
        },
        {
            regex: new RegExp('@"\\n[\\s\\S]*?\\n"@', "gm"),
            css: "script string here"
        },
        {
            regex: new RegExp("@'\\n[\\s\\S]*?\\n'@", "gm"),
            css: "script string single here"
        },
        {
            regex: new RegExp('"(?:\\$\\([^\\)]*\\)|[^"]|`"|"")*[^`]"', "g"),
            css: "string"
        },
        {
            regex: new RegExp("'(?:[^']|'')*'", "g"),
            css: "string single"
        },
        {
            regex: new RegExp("[\\$|@|@@](?:(?:global|script|private|env):)?[A-Z0-9_]+", "gi"),
            css: "variable"
        },
        {
            regex: new RegExp("(?:\\b" + e.replace(/ /g, "\\b|\\b") + ")-[a-zA-Z_][a-zA-Z0-9_]*", "gmi"),
            css: "functions"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gmi"),
            css: "keyword"
        },
        {
            regex: new RegExp("-" + this.getKeywords(b), "gmi"),
            css: "operator value"
        },
        {
            regex: new RegExp("\\[[A-Z_\\[][A-Z0-9_. `,\\[\\]]*\\]", "gi"),
            css: "constants"
        },
        {
            regex: new RegExp("\\s+-(?!" + this.getKeywords(b) + ")[a-zA-Z_][a-zA-Z0-9_]*", "gmi"),
            css: "color1"
        },
        ]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["powershell", "ps", "posh"];
    SyntaxHighlighter.brushes.PowerShell = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var d = "and assert break class continue def del elif else " + "except exec finally for from global if import in is " + "lambda not or pass print raise return try yield while";
        var b = "__import__ abs all any apply basestring bin bool buffer callable " + "chr classmethod cmp coerce compile complex delattr dict dir " + "divmod enumerate eval execfile file filter float format frozenset " + "getattr globals hasattr hash help hex id input int intern " + "isinstance issubclass iter len list locals long map max min next " + "object oct open ord pow print property range raw_input reduce " + "reload repr reversed round set setattr slice sorted staticmethod " + "str sum super tuple type type unichr unicode vars xrange zip";
        var c = "None True False self cls class_";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLinePerlComments,
            css: "comments"
        },
        {
            regex: /^\s*@\w+/gm,
            css: "decorator"
        },
        {
            regex: /(['\"]{3})([^\1])*?\1/gm,
            css: "comments"
        },
        {
            regex: /"(?!")(?:\.|\\\"|[^\""\n])*"/gm,
            css: "string"
        },
        {
            regex: /'(?!')(?:\.|(\\\')|[^\''\n])*'/gm,
            css: "string"
        },
        {
            regex: /\+|\-|\*|\/|\%|=|==/gm,
            css: "keyword"
        },
        {
            regex: /\b\d+\.?\w*/g,
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gmi"),
            css: "functions"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "color1"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["py", "python"];
    SyntaxHighlighter.brushes.Python = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "alias and BEGIN begin break case class def define_method defined do each else elsif " + "END end ensure false for if in module new next nil not or raise redo rescue retry return " + "self super then throw true undef unless until when while yield";
        var c = "Array Bignum Binding Class Continuation Dir Exception FalseClass File::Stat File Fixnum Fload " + "Hash Integer IO MatchData Method Module NilClass Numeric Object Proc Range Regexp String Struct::TMS Symbol " + "ThreadGroup Thread Time TrueClass";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLinePerlComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /\b[A-Z0-9_]+\b/g,
            css: "constants"
        },
        {
            regex: /:[a-z][A-Za-z0-9_]*/g,
            css: "color2"
        },
        {
            regex: /(\$|@@|@)\w+/g,
            css: "variable bold"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "color1"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["ruby", "rails", "ror", "rb"];
    SyntaxHighlighter.brushes.Ruby = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        function b(j) {
            return "\\b([a-z_]|)" + j.replace(/ /g, "(?=:)\\b|\\b([a-z_\\*]|\\*|)") + "(?=:)\\b"
        }
        function e(j) {
            return "\\b" + j.replace(/ /g, "(?!-)(?!:)\\b|\\b()") + ":\\b"
        }
        var g = "ascent azimuth background-attachment background-color background-image background-position " + "background-repeat background baseline bbox border-collapse border-color border-spacing border-style border-top " + "border-right border-bottom border-left border-top-color border-right-color border-bottom-color border-left-color " + "border-top-style border-right-style border-bottom-style border-left-style border-top-width border-right-width " + "border-bottom-width border-left-width border-width border bottom cap-height caption-side centerline clear clip color " + "content counter-increment counter-reset cue-after cue-before cue cursor definition-src descent direction display " + "elevation empty-cells float font-size-adjust font-family font-size font-stretch font-style font-variant font-weight font " + "height left letter-spacing line-height list-style-image list-style-position list-style-type list-style margin-top " + "margin-right margin-bottom margin-left margin marker-offset marks mathline max-height max-width min-height min-width orphans " + "outline-color outline-style outline-width outline overflow padding-top padding-right padding-bottom padding-left padding page " + "page-break-after page-break-before page-break-inside pause pause-after pause-before pitch pitch-range play-during position " + "quotes right richness size slope src speak-header speak-numeral speak-punctuation speak speech-rate stemh stemv stress " + "table-layout text-align top text-decoration text-indent text-shadow text-transform unicode-bidi unicode-range units-per-em " + "vertical-align visibility voice-family volume white-space widows width widths word-spacing x-height z-index";
        var d = "above absolute all always aqua armenian attr aural auto avoid baseline behind below bidi-override black blink block blue bold bolder " + "both bottom braille capitalize caption center center-left center-right circle close-quote code collapse compact condensed " + "continuous counter counters crop cross crosshair cursive dashed decimal decimal-leading-zero digits disc dotted double " + "embed embossed e-resize expanded extra-condensed extra-expanded fantasy far-left far-right fast faster fixed format fuchsia " + "gray green groove handheld hebrew help hidden hide high higher icon inline-table inline inset inside invert italic " + "justify landscape large larger left-side left leftwards level lighter lime line-through list-item local loud lower-alpha " + "lowercase lower-greek lower-latin lower-roman lower low ltr marker maroon medium message-box middle mix move narrower " + "navy ne-resize no-close-quote none no-open-quote no-repeat normal nowrap n-resize nw-resize oblique olive once open-quote outset " + "outside overline pointer portrait pre print projection purple red relative repeat repeat-x repeat-y rgb ridge right right-side " + "rightwards rtl run-in screen scroll semi-condensed semi-expanded separate se-resize show silent silver slower slow " + "small small-caps small-caption smaller soft solid speech spell-out square s-resize static status-bar sub super sw-resize " + "table-caption table-cell table-column table-column-group table-footer-group table-header-group table-row table-row-group teal " + "text-bottom text-top thick thin top transparent tty tv ultra-condensed ultra-expanded underline upper-alpha uppercase upper-latin " + "upper-roman url visible wait white wider w-resize x-fast x-high x-large x-loud x-low x-slow x-small x-soft xx-large xx-small yellow";
        var i = "[mM]onospace [tT]ahoma [vV]erdana [aA]rial [hH]elvetica [sS]ans-serif [sS]erif [cC]ourier mono sans serif";
        var c = "!important !default";
        var f = "@import @extend @debug @warn @if @for @while @mixin @include";
        var h = SyntaxHighlighter.regexLib;
        this.regexList = [{
            regex: h.multiLineCComments,
            css: "comments"
        },
        {
            regex: h.singleLineCComments,
            css: "comments"
        },
        {
            regex: h.doubleQuotedString,
            css: "string"
        },
        {
            regex: h.singleQuotedString,
            css: "string"
        },
        {
            regex: /\#[a-fA-F0-9]{3,6}/g,
            css: "value"
        },
        {
            regex: /\b(-?\d+)(\.\d+)?(px|em|pt|\:|\%|)\b/g,
            css: "value"
        },
        {
            regex: /\$\w+/g,
            css: "variable"
        },
        {
            regex: new RegExp(this.getKeywords(c), "g"),
            css: "color3"
        },
        {
            regex: new RegExp(this.getKeywords(f), "g"),
            css: "preprocessor"
        },
        {
            regex: new RegExp(b(g), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(e(d), "g"),
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(i), "g"),
            css: "color1"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["sass", "scss"];
    SyntaxHighlighter.brushes.Sass = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "val sealed case def true trait implicit forSome import match object null finally super " + "override try lazy for var catch throw type extends class while with new final yield abstract " + "else do if return protected private this package false";
        var b = "[_:=><%#@]+";
        this.regexList = [{
            regex: SyntaxHighlighter.regexLib.singleLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineCComments,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineSingleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineDoubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.singleQuotedString,
            css: "string"
        },
        {
            regex: /0x[a-f0-9]+|\d+(\.\d+)?/gi,
            css: "value"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gm"),
            css: "keyword"
        },
        {
            regex: new RegExp(b, "gm"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["scala"];
    SyntaxHighlighter.brushes.Scala = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var c = "abs avg case cast coalesce convert count current_timestamp " + "current_user day isnull left lower month nullif replace right " + "session_user space substring sum system_user upper user year";
        var d = "absolute action add after alter as asc at authorization begin bigint " + "binary bit by cascade char character check checkpoint close collate " + "column commit committed connect connection constraint contains continue " + "create cube current current_date current_time cursor database date " + "deallocate dec decimal declare default delete desc distinct double drop " + "dynamic else end end-exec escape except exec execute false fetch first " + "float for force foreign forward free from full function global goto grant " + "group grouping having hour ignore index inner insensitive insert instead " + "int integer intersect into is isolation key last level load local max min " + "minute modify move name national nchar next no numeric of off on only " + "open option order out output partial password precision prepare primary " + "prior privileges procedure public read real references relative repeatable " + "restrict return returns revoke rollback rollup rows rule schema scroll " + "second section select sequence serializable set size smallint static " + "statistics table temp temporary then time timestamp to top transaction " + "translation trigger true truncate uncommitted union unique update values " + "varchar varying view when where with work";
        var b = "all and any between cross in join like not null or outer some";
        this.regexList = [{
            regex: /--(.*)$/gm,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineDoubleQuotedString,
            css: "string"
        },
        {
            regex: SyntaxHighlighter.regexLib.multiLineSingleQuotedString,
            css: "string"
        },
        {
            regex: new RegExp(this.getKeywords(c), "gmi"),
            css: "color2"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gmi"),
            css: "color1"
        },
        {
            regex: new RegExp(this.getKeywords(d), "gmi"),
            css: "keyword"
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["sql"];
    SyntaxHighlighter.brushes.Sql = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        var b = "AddHandler AddressOf AndAlso Alias And Ansi As Assembly Auto " + "Boolean ByRef Byte ByVal Call Case Catch CBool CByte CChar CDate " + "CDec CDbl Char CInt Class CLng CObj Const CShort CSng CStr CType " + "Date Decimal Declare Default Delegate Dim DirectCast Do Double Each " + "Else ElseIf End Enum Erase Error Event Exit False Finally For Friend " + "Function Get GetType GoSub GoTo Handles If Implements Imports In " + "Inherits Integer Interface Is Let Lib Like Long Loop Me Mod Module " + "MustInherit MustOverride MyBase MyClass Namespace New Next Not Nothing " + "NotInheritable NotOverridable Object On Option Optional Or OrElse " + "Overloads Overridable Overrides ParamArray Preserve Private Property " + "Protected Public RaiseEvent ReadOnly ReDim REM RemoveHandler Resume " + "Return Select Set Shadows Shared Short Single Static Step Stop String " + "Structure Sub SyncLock Then Throw To True Try TypeOf Unicode Until " + "Variant When While With WithEvents WriteOnly Xor";
        this.regexList = [{
            regex: /'.*$/gm,
            css: "comments"
        },
        {
            regex: SyntaxHighlighter.regexLib.doubleQuotedString,
            css: "string"
        },
        {
            regex: /^\s*#.*$/gm,
            css: "preprocessor"
        },
        {
            regex: new RegExp(this.getKeywords(b), "gm"),
            css: "keyword"
        }];
        this.forHtmlScript(SyntaxHighlighter.regexLib.aspScriptTags)
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["vb", "vbnet"];
    SyntaxHighlighter.brushes.Vb = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})(); (function() {
    SyntaxHighlighter = SyntaxHighlighter || (typeof require !== "undefined" ? require("shCore").SyntaxHighlighter: null);
    function a() {
        function b(f, j) {
            var g = SyntaxHighlighter.Match,
            i = f[0],
            d = new XRegExp("(&lt;|<)[\\s\\/\\?]*(?<name>[:\\w-\\.]+)", "xg").exec(i),
            c = [];
            if (f.attributes != null) {
                var e, h = new XRegExp("(?<name> [\\w:\\-\\.]+)" + "\\s*=\\s*" + "(?<value> \".*?\"|'.*?'|\\w+)", "xg");
                while ((e = h.exec(i)) != null) {
                    c.push(new g(e.name, f.index + e.index, "color1"));
                    c.push(new g(e.value, f.index + e.index + e[0].indexOf(e.value), "string"))
                }
            }
            if (d != null) {
                c.push(new g(d.name, f.index + d[0].indexOf(d.name), "keyword"))
            }
            return c
        }
        this.regexList = [{
            regex: new XRegExp("(\\&lt;|<)\\!\\[[\\w\\s]*?\\[(.|\\s)*?\\]\\](\\&gt;|>)", "gm"),
            css: "color2"
        },
        {
            regex: SyntaxHighlighter.regexLib.xmlComments,
            css: "comments"
        },
        {
            regex: new XRegExp("(&lt;|<)[\\s\\/\\?]*(\\w+)(?<attributes>.*?)[\\s\\/\\?]*(&gt;|>)", "sg"),
            func: b
        }]
    }
    a.prototype = new SyntaxHighlighter.Highlighter();
    a.aliases = ["xml", "xhtml", "xslt", "html"];
    SyntaxHighlighter.brushes.Xml = a;
    typeof(exports) != "undefined" ? exports.Brush = a: null
})();