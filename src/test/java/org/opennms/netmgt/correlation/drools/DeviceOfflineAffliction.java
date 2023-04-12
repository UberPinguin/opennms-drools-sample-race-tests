package org.opennms.netmgt.correlation.drools;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.drools.core.common.DefaultFactHandle;

public class DeviceOfflineAffliction extends DefaultFactHandle {

  String location;

  public DeviceOfflineAffliction(String location)
  {
    this.location = location;
  }

  public DeviceOfflineAffliction() {}

  public String getLocation() {
    return location;
  }

  public DeviceOfflineAffliction setLocation(String location) {
    this.location = location;
    return this;
  }

  @Override
  public String toString() {
    return "DeviceOfflineAffliction{" +
            "location='" + location + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DeviceOfflineAffliction)) return false;

    DeviceOfflineAffliction that = (DeviceOfflineAffliction) o;

    return new EqualsBuilder().appendSuper(super.equals(o)).append(getLocation(), that.getLocation()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(getLocation()).toHashCode();
  }
}