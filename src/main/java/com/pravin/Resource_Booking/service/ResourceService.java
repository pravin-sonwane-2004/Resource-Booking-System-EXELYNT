package com.pravin.Resource_Booking.service;

import com.pravin.Resource_Booking.dto.resource.ResourceRequest;
import com.pravin.Resource_Booking.dto.resource.ResourceResponse;
import com.pravin.Resource_Booking.entity.Resource;
import com.pravin.Resource_Booking.exception.ResourceNotFoundException;
import com.pravin.Resource_Booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceResponse> getAll() {
        return resourceRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ResourceResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setName(request.name().trim());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setAvailable(request.available());
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = find(id);
        resource.setName(request.name().trim());
        if (request.description() != null) {
            resource.setDescription(request.description());
        }
        resource.setType(request.type());
        resource.setAvailable(request.available());
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        resourceRepository.delete(find(id));
    }

    private Resource find(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.isAvailable(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
