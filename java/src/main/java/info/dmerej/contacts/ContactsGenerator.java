package info.dmerej.contacts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ContactsGenerator {
  public Stream<Contact> generateContacts(int count) {
    List<Contact> contacts = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      String name = "Contact " + (i + 1);
      String email = "email-" + (i + 1) + "@tld";
      contacts.add(new Contact(name, email));
    }

    return contacts.stream();
  }
}
