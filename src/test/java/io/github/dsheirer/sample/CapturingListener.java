package io.github.dsheirer.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapturingListener<T> implements Listener<T> {

  private final ArrayList<T> values = new ArrayList<>();

  @Override
  public synchronized void receive(T t) {
    values.add(t);
  }

  public synchronized List<T> capturedValues() {
    return Collections.unmodifiableList(values);
  }
}
