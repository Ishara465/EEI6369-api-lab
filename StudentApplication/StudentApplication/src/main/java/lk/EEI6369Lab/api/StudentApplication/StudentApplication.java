package lk.EEI6369Lab.api.StudentApplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableJpaRepositories(
        basePackageClasses = StudentApplication.class,
        considerNestedRepositories = true
)
public class StudentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentApplication.class, args);
    }

    // Database entity
    @Entity(name = "Student")
    @Table(name = "students")
    public static class Student {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 100)
        private String name;

        @Column(nullable = false, unique = true, length = 254)
        private String email;

        protected Student() {
        }

        public Student(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Spring Data implements this interface at runtime.
    public interface StudentRepository extends JpaRepository<Student, Long> {
    }

    // A request contains editable fields; the database generates the ID.
    public record StudentRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email
    ) {
    }

    @RestController
    @RequestMapping("/api/students")
    public static class StudentController {

        private final StudentRepository repository;

        public StudentController(StudentRepository repository) {
            this.repository = repository;
        }

        // CREATE: POST /api/students
        @PostMapping
        public ResponseEntity<Student> createStudent(
                @Valid @RequestBody StudentRequest request) {
            Student student = new Student(request.name(), request.email());
            Student saved = repository.saveAndFlush(student);

            return ResponseEntity
                    .created(URI.create("/api/students/" + saved.getId()))
                    .body(saved);
        }

        // READ ALL: GET /api/students
        @GetMapping
        public List<Student> getAllStudents() {
            return repository.findAll();
        }

        // READ ONE: GET /api/students/{id}
        @GetMapping("/{id}")
        public Student getStudent(@PathVariable("id") Long id) {
            return findStudent(id);
        }

        // UPDATE: PUT /api/students/{id}
        @PutMapping("/{id}")
        public Student updateStudent(
                @PathVariable("id") Long id,
                @Valid @RequestBody StudentRequest request) {
            Student student = findStudent(id);
            student.setName(request.name());
            student.setEmail(request.email());

            return repository.saveAndFlush(student);
        }

        // DELETE: DELETE /api/students/{id}
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteStudent(@PathVariable("id") Long id) {
            repository.delete(findStudent(id));
            return ResponseEntity.noContent().build();
        }

        private Student findStudent(Long id) {
            return repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Student not found"));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, String>> handleConflict() {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Student data conflicts with an existing record. Use a unique email address.")
            );
        }
    }
}
