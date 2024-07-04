import htmx from "htmx.org/dist/htmx.esm";
import "idiomorph";
import _hyperscript from "hyperscript.org";
import "bootstrap";

window.htmx = htmx;

require("htmx-ext-sse");

_hyperscript.browserInit();
