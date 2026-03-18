package ge.dola.talanti.club.event;

public record CalendarEventCreatedEvent(Long clubId, String eventType, String title) {}