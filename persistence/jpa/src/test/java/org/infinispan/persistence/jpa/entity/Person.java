package org.infinispan.persistence.jpa.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import org.infinispan.protostream.annotations.ProtoField;

@Entity
public class Person {

   @Id
   private String id;

   private String name;

   @ElementCollection(fetch = FetchType.EAGER)
   private Set<String> nickNames;

   @Embedded
   @AttributeOverrides({ @AttributeOverride(name = "zipCode", column = @Column(name = "zip")) })
   private Address address;

   @ElementCollection(fetch = FetchType.EAGER)
   private Set<Address> secondaryAddresses;

   @ProtoField(1)
   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   @ProtoField(2)
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @ProtoField(number = 3, collectionImplementation = HashSet.class)
   public Set<String> getNickNames() {
      return nickNames;
   }

   public void setNickNames(Set<String> nickNames) {
      this.nickNames = nickNames;
   }

   @ProtoField(4)
   public Address getAddress() {
      return address;
   }

   public void setAddress(Address address) {
      this.address = address;
   }

   @ProtoField(number = 5, collectionImplementation = HashSet.class)
   public Set<Address> getSecondaryAddresses() {
      return secondaryAddresses;
   }

   public void setSecondaryAddresses(Set<Address> secondaryAddresses) {
      this.secondaryAddresses = secondaryAddresses;
   }

   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      final Person person = (Person) o;

      if (id != null ? !id.equals(person.getId()) : person.getId() != null)
         return false;
      if (name != null ? !name.equals(person.getName()) : person.getName() != null)
         return false;
      if ( (nickNames != null && !nickNames.isEmpty()) ? !nickNames.equals(person.getNickNames()) :
         (person.getNickNames() != null && !person.getNickNames().isEmpty()))
         return false;
      if (address != null ? !address.equals(person.getAddress()) : person.getAddress() != null)
         return false;
      if ( (secondaryAddresses != null  && !secondaryAddresses.isEmpty() ) ? !secondaryAddresses.equals(person.getSecondaryAddresses()) :
         (person.getSecondaryAddresses() != null && !person.getSecondaryAddresses().isEmpty()))
         return false;

      return true;
   }

   public int hashCode() {
      final int prime = 31;
      int result;
      result = (id != null ? id.hashCode() : 0);
      result = prime * result + (name != null ? name.hashCode() : 0);
      result = prime * result + (nickNames != null ? nickNames.hashCode() : 0);
      result = prime * result + (address != null ? address.hashCode() : 0);
      result = prime * result + (secondaryAddresses != null ? secondaryAddresses.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "Person{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", nickNames=" + nickNames +
            ", address=" + address +
            ", secondaryAddresses=" + secondaryAddresses +
            '}';
   }
}
