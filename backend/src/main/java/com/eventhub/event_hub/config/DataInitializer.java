package com.eventhub.event_hub.config;

import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.models.Event;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import com.eventhub.event_hub.repositories.UserRepository;
import com.eventhub.event_hub.utils.Logins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Startup tasks, both opt-in through configuration:
 * <ul>
 *   <li>bootstrap admin: the only way to get an ADMIN account (public registration never creates one);</li>
 *   <li>demo data: a small catalogue so a fresh environment is not an empty page.</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminLogin;
    private final String adminPassword;
    private final boolean seedEnabled;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           EventRepository eventRepository,
                           PasswordEncoder passwordEncoder,
                           Clock clock,
                           @Value("${app.bootstrap-admin.login:}") String adminLogin,
                           @Value("${app.bootstrap-admin.password:}") String adminPassword,
                           @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminLogin = adminLogin;
        this.adminPassword = adminPassword;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapAdmin();
        if (seedEnabled) {
            seedDemoData();
        }
    }

    private void bootstrapAdmin() {
        if (adminLogin == null || adminLogin.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        String login = Logins.normalize(adminLogin);
        if (userRepository.existsByLogin(login)) {
            return;
        }
        userRepository.save(new User(login, passwordEncoder.encode(adminPassword), UserRole.ADMIN));
        log.info("Bootstrap admin account '{}' created", login);
    }

    private void seedDemoData() {
        if (categoryRepository.count() > 0 || eventRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);

        Category tech = categoryRepository.save(new Category("Technology"));
        Category music = categoryRepository.save(new Category("Music"));
        Category business = categoryRepository.save(new Category("Business"));
        Category workshops = categoryRepository.save(new Category("Workshops"));
        Category sports = categoryRepository.save(new Category("Sports"));

        seed(now, 6, 19, "Barcelona Java Meetup",
                "An evening of short talks on modern Java: virtual threads, records and pattern matching, followed by networking.",
                "Tech Hub Barcelona", tech, 120, "0.00", 87);
        seed(now, 12, 9, "Spring Boot in Production",
                "A full-day workshop on observability, resilience and zero-downtime deployments for Spring Boot services.",
                "Palau de Congressos, Barcelona", workshops, 60, "149.00", 41);
        seed(now, 18, 20, "Indie Rock Night",
                "Three emerging bands, one stage. Doors open at 19:00.",
                "Sala Apolo, Barcelona", music, 300, "25.00", 212);
        seed(now, 25, 10, "Startup Pitch Day",
                "Ten early-stage startups pitch to a panel of investors. Open Q&A afterwards.",
                "Mobile World Centre", business, 200, "35.00", 64);
        seed(now, 32, 18, "Cloud Native Summit",
                "Kubernetes, platform engineering and FinOps from teams running it in anger.",
                "Fira Barcelona", tech, 500, "89.00", 150);
        seed(now, 40, 21, "Jazz at the Park",
                "An open-air jazz evening with local and international artists.",
                "Parc de la Ciutadella", music, 250, "18.00", 249);
        seed(now, 47, 8, "City 10K Run",
                "A flat, fast 10K through the city centre. Timing chip and finisher medal included.",
                "Passeig de Gracia", sports, 1000, "22.00", 530);
        seed(now, 55, 10, "Hands-on Docker & CI/CD",
                "Build, test and ship a containerised app with a real pipeline. Bring a laptop.",
                "Coworking Poblenou", workshops, 30, "79.00", 12);
        seed(now, 63, 19, "Product Management Evening",
                "Practitioners share how they prioritise, measure and say no.",
                "Tech Hub Barcelona", business, 90, "0.00", 90);
        seed(now, 75, 20, "Electronic Music Festival Warm-up",
                "Local DJs set the tone for the summer season.",
                "Razzmatazz Annex", music, 400, "30.00", 120);

        log.info("Demo catalogue loaded");
    }

    private void seed(LocalDateTime now, int daysAhead, int hour, String title, String description,
                      String location, Category category, int capacity, String price, int sold) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setStartsAt(now.toLocalDate().plusDays(daysAhead).atTime(LocalTime.of(hour, 0)));
        event.setPrice(new BigDecimal(price));
        event.setCategory(category);
        event.setCreatedAt(now);
        event.changeCapacity(capacity);
        event.reserve(Math.min(sold, capacity), now);
        eventRepository.save(event);
    }
}
