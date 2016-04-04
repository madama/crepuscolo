package net.etalia.crepuscolo.queue;

import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class SQSNotificationReadQueue<T> extends SQSReadQueue<T> {

}
