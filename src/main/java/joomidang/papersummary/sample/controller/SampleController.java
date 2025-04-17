package joomidang.papersummary.sample.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joomidang.papersummary.sample.dto.SampleRequest;
import joomidang.papersummary.sample.dto.SampleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestDocs을 만들기 위한 참고용 Controller 기본적인 crud지원
 */
@RestController
@RequestMapping("/api/samples")
public class SampleController {

    private final Map<Long, SampleResponse> samples = new HashMap<>();
    private long nextId = 1;

    @GetMapping
    public ResponseEntity<List<SampleResponse>> getAllSamples() {
        return ResponseEntity.ok(new ArrayList<>(samples.values()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SampleResponse> getSampleById(@PathVariable Long id) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(samples.get(id));
    }

    @PostMapping
    public ResponseEntity<SampleResponse> createSample(@RequestBody SampleRequest request) {
        long id = nextId++;
        SampleResponse response = new SampleResponse(id, request.getName(), request.getDescription(),
                request.getCategory());
        samples.put(id, response);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SampleResponse> updateSample(@PathVariable Long id, @RequestBody SampleRequest request) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        SampleResponse response = new SampleResponse(id, request.getName(), request.getDescription(),
                request.getCategory());
        samples.put(id, response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSample(@PathVariable Long id) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        samples.remove(id);
        return ResponseEntity.noContent().build();
    }
}