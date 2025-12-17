package dev.datastar;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Datastar {
  public record Event(String name, String data, String id, Long reconnectDelay) {
    public Event withId(String id) {
      return new Event(this.name, this.data, id, this.reconnectDelay);
    }

    /** in milliseconds */
    public Event withReconnectDelay(Long reconnectDelay) {
      return new Event(this.name, this.data, this.id, reconnectDelay);
    }
  }

  Datastar() {
  }

  public static PatchElements patchElements() {
    return new PatchElements();
  }

  public static PatchSignals patchSignals() {
    return new PatchSignals();
  }

  public static Event removeElements(Collection<String> selectors) {
    return new PatchElements().select(selectors.stream().collect(Collectors.joining(" ")))
        .remove();
  }

  public static Event removeElements(String... selectors) {
    return new PatchElements().select(Stream.of(selectors).collect(Collectors.joining(" ")))
        .remove();
  }

  /**
   * You can't remove signals with this method. Use
   * {@link Datastar#patchSignals()} and set the signals you want to be removed to
   * <code>null</code>
   * 
   * @return nothing
   */
  @Deprecated
  public static Void removeSignals() {
    return null;
  }

  public static ExecuteScript executeScript() {
    return new ExecuteScript();
  }

  /**
   * Don't use this class. Use {@link Datastar#patchElements()} instead
   */
  public static class PatchElements {
    private String selector;
    private boolean useViewTransition;
    private String mode;
    private String namespace;

    PatchElements() {
    }

    /** the target as CSS selector */
    public PatchElements select(String selector) {
      this.selector = selector;
      return this;
    }

    public PatchElements useViewTransition() {
      this.useViewTransition = true;
      return this;
    }

    /**
     * interpret the elements as SVG elements within the SVG namespace.
     * 
     * Can't be mixed with elements from other namespaces.
     */
    public PatchElements asSVG() {
      this.namespace = "svg";
      return this;
    }

    /**
     * interpret the elements as MathML elements within the MathML namespace.
     * 
     * Can't be mixed with elements from other namespaces.
     */
    public PatchElements asMathML() {
      this.namespace = "mathml";
      return this;
    }

    /** outer of target element and preserve state */
    public Event replace(String htmlElements) {
      return createEvent(htmlElements);
    }

    /** of the target element */
    public Event replaceInnerHtml(String htmlElements) {
      this.mode = "inner";
      return createEvent(htmlElements);
    }

    /** of target element and reset state */
    public Event replaceAndReset(String htmlElements) {
      this.mode = "replace";
      return createEvent(htmlElements);
    }

    /** of the target's first child as sibling */
    public Event prependToChildren(String htmlElements) {
      this.mode = "prepend";
      return createEvent(htmlElements);
    }

    /** of the target's last child as sibling */
    public Event appendToChildren(String htmlElements) {
      this.mode = "append";
      return createEvent(htmlElements);
    }

    /** of the target as sibling */
    public Event beforeSelector(String htmlElements) {
      this.mode = "before";
      return createEvent(htmlElements);
    }

    /** of the target as sibling */
    public Event afterSelector(String htmlElements) {
      this.mode = "after";
      return createEvent(htmlElements);
    }

    /** the target itself and children */
    public Event remove() {
      this.mode = "remove";
      return createEvent(null);
    }

    private Event createEvent(String htmlElements) {
      var event = new StringBuilder();
      if (this.selector != null)
        event.append("selector ").append(this.selector).append('\n');
      if (this.mode != null)
        event.append("mode ").append(this.mode).append('\n');
      if (this.useViewTransition)
        event.append("useViewTransition true\n");
      if (this.namespace != null)
        event.append("namespace ").append(this.namespace).append('\n');
      if (htmlElements != null)
        event.append(htmlElements.lines().filter(Predicate.not(String::isEmpty))
            .map(line -> "elements " + line).collect(Collectors.joining("\n")));
      return new Event("datastar-patch-elements", event.toString(), null, null);
    }
  }

  /**
   * Don't use this class. Use {@link Datastar#patchSignals()} instead
   */
  public static final class PatchSignals {
    private boolean onlyIfMissing;

    PatchSignals() {
    }

    /** when not already defined on the client */
    public PatchSignals onlyIfMissing() {
      this.onlyIfMissing = true;
      return this;
    }

    public Event withSignals(String signalsAsJson) {
      var event = new StringBuilder();
      if (this.onlyIfMissing)
        event.append("onlyIfMissing true\n");
      signalsAsJson.lines().filter(Predicate.not(String::isEmpty))
          .map(line -> "signals " + line + '\n').forEach(event::append);
      return new Event("datastar-patch-signals", event.toString(), null, null);
    }
  }

  /**
   * Don't use this class. Use {@link Datastar#executeScript()} instead
   */
  public static final class ExecuteScript {
    private Map<String, String> attributes;
    private boolean autoRemove;

    ExecuteScript() {
      this.attributes = new HashMap<>(4);
      this.autoRemove = true;
    }

    /** add (valid) HTML attributes to the &lt;script&gt; */
    public ExecuteScript withAttributes(Map<String, String> attibutes) {
      this.attributes = attibutes;
      return this;
    }

    /** the script after it is executed */
    public ExecuteScript doNotRemove() {
      this.autoRemove = false;
      return this;
    }

    /**
     * DO NOT wrap the script itself into a HTML-&lt;script&gt; tag.
     * 
     * @param javaScript plain JavaScript
     * @return the constructed event
     */
    public Event withScript(String javaScript) {
      var script = new StringBuilder("<script");
      if (this.autoRemove)
        this.attributes.put("data-effect", "el.remove()");
      this.attributes.entrySet().stream()
          .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
          .forEach(attribute -> script.append(' ').append(attribute));
      script.append(">").append(javaScript).append("</script>");
      return new PatchElements().select("body").appendToChildren(script.toString());
    }
  }
}
