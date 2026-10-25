# Annotations Cheat Sheet

Every annotation used in this course, grouped by job. **S#** = the session where it's introduced.
Remember: an annotation is a *label*. Some framework code reads it and does the work.

## Bootstrapping & configuration

| Annotation | S# | Put it on | Meaning |
|------------|----|-----------|---------|
| `@SpringBootApplication` | 1 | main class | `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@Configuration` | 2 | class | "This class contains `@Bean` methods" |
| `@Bean` | 2 | method | "Register the returned object as a bean" (for classes you can't annotate) |
| `@Value("${key:default}")` | 2 | constructor param | Inject one property value |
| `@ConfigurationProperties(prefix = "app.x")` | 5 | record/class | Bind a group of properties into an object |
| `@ConfigurationPropertiesScan` | 5 | main class | Find and register `@ConfigurationProperties` types |
| `@Profile("postgres")` | 2/3 | bean class | Only create this bean when the profile is active |

## Beans & dependency injection

| Annotation | S# | Meaning |
|------------|----|---------|
| `@Component` | 2 | Generic bean |
| `@Service` | 2 | Bean in the business-logic layer |
| `@Repository` | 2 | Bean in the data layer (+ DB exception translation) |
| `@Controller` / `@RestController` | 1 | Web bean; `@RestController` writes return values as the response body |
| `@Autowired` | 2 | Inject here. **Not needed** on a single constructor; prefer constructor injection |
| `@Primary` | 2 | Default choice when several beans match |
| `@Qualifier("name")` | 2 | Pick a specific bean by name at the injection point |

## Web (REST)

| Annotation | S# | Meaning |
|------------|----|---------|
| `@RequestMapping("/api/todos")` | 1 | URL prefix for a controller |
| `@GetMapping` `@PostMapping` `@PutMapping` `@PatchMapping` `@DeleteMapping` | 1 | Map an HTTP method + path to a Java method |
| `@PathVariable` | 1 | Value from the path: `/todos/{id}` |
| `@RequestParam` | 1 | Value from the query string: `?completed=true` |
| `@RequestBody` | 1 | JSON body → Java object |
| `@ResponseStatus(HttpStatus.CREATED)` | 1 | Status code for a successful call |
| `@RestControllerAdvice` | 1 | Global exception handling for all controllers |
| `@ExceptionHandler(X.class)` | 1 | "Call me when X is thrown" |

## Validation (Jakarta Bean Validation)

| Annotation | S# | Meaning |
|------------|----|---------|
| `@Valid` | 1 | "Check the rules on this object" (nothing is checked without it!) |
| `@NotBlank` | 1 | Not null, not empty, not only spaces |
| `@NotNull` | 1 | Not null |
| `@Size(min, max)` | 1 | Length limits |
| `@Pattern(regexp)` | 4 | Must match a regex |

## Persistence (JPA / Spring Data)

| Annotation | S# | Meaning |
|------------|----|---------|
| `@Entity` | 3 | This class maps to a table |
| `@Table(name = "app_user")` | 3 | Custom table name |
| `@Id` | 3 | Primary key |
| `@GeneratedValue(strategy = IDENTITY)` | 3 | DB generates the id |
| `@Column(nullable, length, unique, updatable)` | 3 | Column details |
| `@Enumerated(EnumType.STRING)` | 3 | Store enum names, not numbers |
| `@ManyToOne` + `@JoinColumn` | 4 | Foreign key relationship |
| `@Query("JPQL")` + `@Param` | 3 | Hand-written query on a repository method |
| `@Transactional` / `(readOnly = true)` | 3 | All-or-nothing unit of work; enables dirty checking |

## Security

| Annotation | S# | Meaning |
|------------|----|---------|
| `@EnableMethodSecurity` | 4 | Turn on `@PreAuthorize` & friends |
| `@PreAuthorize("hasRole('ADMIN')")` | 4 | Check before the method runs |
| `@AuthenticationPrincipal` | 4 | Inject the logged-in `UserDetails` |

## Writing your own annotation

| Annotation | S# | Meaning |
|------------|----|---------|
| `@interface` | 5 | Declares an annotation type |
| `@Target({METHOD, TYPE})` | 5 | Where it may be placed |
| `@Retention(RUNTIME)` | 5 | Keep it visible at runtime (**required** if code reads it while running) |
| `@Documented` | 5 | Show it in Javadoc |

## GraphQL

| Annotation | S# | Meaning |
|------------|----|---------|
| `@QueryMapping` | 6 | Resolver for a field on `type Query` |
| `@MutationMapping` | 6 | Resolver for a field on `type Mutation` |
| `@SchemaMapping(typeName, field)` | 6 | Resolver for a field on any type |
| `@BatchMapping` | 6 | Batched resolver (fixes N+1) |
| `@Argument` | 6 | Bind a GraphQL argument |
| `@GraphQlExceptionHandler` | 6 | Exception → `GraphQLError` |

## Lombok (compile-time code generation)

| Annotation | Generates |
|------------|-----------|
| `@Getter` / `@Setter` | getters / setters |
| `@NoArgsConstructor` | empty constructor (JPA needs it) |
| `@RequiredArgsConstructor` | constructor for `final` fields; a shortcut for constructor injection |

## Testing

| Annotation | Meaning |
|------------|---------|
| `@Test` | A JUnit test method |
| `@SpringBootTest` | Start the whole application context |
| `@AutoConfigureMockMvc` | Provide a `MockMvc` for fake HTTP requests |
| `@AutoConfigureHttpGraphQlTester` | Provide an `HttpGraphQlTester` |
| `@DataJpaTest` | Start only JPA + an in-memory DB |
