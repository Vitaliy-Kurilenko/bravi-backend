package ua.com.bravi.bravi.seller.catalog.attributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.dictionaries.api.DictionariesApi;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeOptionView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributePage;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeTemplateView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.catalog.attributes.api.CategoryAttributeView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributesView;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.Attribute;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeInheritance;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeOption;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeOrder;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSearchQuery;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSortBy;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValuePolicy;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.AttributeAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.AttributeInUseException;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.AttributeSpecifications;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.IAttributeEntityRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.IAttributeOptionEntityRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.IAttributeTemplateOptionRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.IAttributeTemplateRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.ICategoryAttributeEntityRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.IProductAttributeValueEntityRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeOptionEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateOptionEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.CategoryAttributeEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.ProductAttributeValueEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.mapper.AttributeEntityMapper;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.mapper.AttributeOptionEntityMapper;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryPathEntry;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;
import ua.com.bravi.bravi.shared.util.ValidationPatterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns attribute definitions, their category bindings and the values products carry for them.
 * Product values live here rather than in the product aggregate because validating them needs the
 * definitions; products reach this service through {@code AttributesApi}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeService implements AttributesApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int SUGGESTION_LIMIT = 20;

    private static final String FIELD_CODE = "code";
    private static final String FIELD_ATTRIBUTES = "attributes";
    private static final String FIELD_ATTRIBUTE_IDS = "attribute_ids";
    private static final String FIELD_TEMPLATE_CODES = "template_codes";

    private final IAttributeEntityRepository attributeRepository;
    private final IAttributeOptionEntityRepository optionRepository;
    private final ICategoryAttributeEntityRepository bindingRepository;
    private final IProductAttributeValueEntityRepository valueRepository;
    private final IAttributeTemplateRepository templateRepository;
    private final IAttributeTemplateOptionRepository templateOptionRepository;
    private final AttributeEntityMapper attributeEntityMapper;
    private final AttributeOptionEntityMapper attributeOptionEntityMapper;
    private final CategoriesApi categoriesApi;
    private final DictionariesApi dictionariesApi;

    // ---------------------------------------------------------------- library

    @Override
    public List<AttributeTemplateView> listTemplates(Long storeId, String search) {
        List<AttributeTemplateEntity> templates = templateRepository.findByActiveTrueOrderBySortOrderAscCodeAsc()
                .stream()
                .filter(template -> matchesSearch(template, search))
                .toList();
        if (templates.isEmpty()) {
            return List.of();
        }

        Map<Long, List<AttributeTemplateOptionEntity>> optionsByTemplate = templateOptionRepository
                .findByTemplateIdInOrderBySortOrderAsc(templates.stream().map(AttributeTemplateEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(AttributeTemplateOptionEntity::getTemplateId));

        Set<String> adopted = attributeRepository
                .findByStoreIdAndTemplateCodeIn(storeId, templates.stream().map(AttributeTemplateEntity::getCode).toList())
                .stream()
                .map(AttributeEntity::getTemplateCode)
                .collect(Collectors.toSet());

        return templates.stream()
                .map(template -> attributeEntityMapper.toTemplateView(
                        template,
                        adopted.contains(template.getCode()),
                        attributeOptionEntityMapper.toTemplateViews(
                                optionsByTemplate.getOrDefault(template.getId(), List.of()))))
                .toList();
    }

    // ------------------------------------------------------------ definitions

    @Override
    public AttributePage search(Long storeId, AttributeSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        AttributeSortBy sortBy = query.sortBy() != null ? query.sortBy() : AttributeSortBy.NAME;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.ASC;

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<AttributeEntity> result =
                attributeRepository.findAll(AttributeSpecifications.forStore(storeId, query), pageable);
        List<AttributeView> data = toViews(result.getContent());

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new AttributePage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public AttributeView getByPublicId(Long storeId, String publicId) {
        return toView(requireOwned(storeId, publicId));
    }

    @Override
    @Transactional
    public AttributeView create(Long storeId, Attribute attribute, List<AttributeOption> options) {
        AttributeEntity entity = attributeEntityMapper.toEntity(attribute);
        entity.setStoreId(storeId);
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ATTRIBUTE_PREFIX));
        entity.setCode(requireValidCode(attribute.code(), FIELD_CODE));
        applyDefaults(entity);
        requireValueType(entity);
        requireKnownUnit(entity);

        AttributeEntity saved = save(entity, FIELD_CODE);
        List<AttributeOptionEntity> savedOptions = replaceOptions(saved, options);

        log.info("Attribute created storeId={} attributeId={} publicId={} code={} valueType={} options={}",
                storeId, saved.getId(), saved.getPublicId(), saved.getCode(), saved.getValueType(),
                savedOptions.size());
        return attributeEntityMapper.toView(saved, attributeOptionEntityMapper.toViews(savedOptions));
    }

    @Override
    @Transactional
    public void update(Long storeId, String publicId, Attribute patch) {
        AttributeEntity entity = requireOwned(storeId, publicId);
        attributeEntityMapper.updateEntity(entity, patch);
        requireKnownUnit(entity);

        // A global attribute reaches every product on its own, so its bindings become meaningless.
        if (entity.getScope() == AttributeScope.GLOBAL) {
            List<CategoryAttributeEntity> obsolete = bindingRepository.findByAttributeId(entity.getId());
            if (!obsolete.isEmpty()) {
                bindingRepository.deleteAll(obsolete);
                obsolete.stream()
                        .map(CategoryAttributeEntity::getCategoryId)
                        .distinct()
                        .forEach(this::resequenceBindings);
            }
        }
        attributeRepository.flush();
        log.info("Attribute updated storeId={} publicId={}", storeId, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, String publicId) {
        AttributeEntity entity = requireOwned(storeId, publicId);
        if (valueRepository.existsByAttributeId(entity.getId())) {
            log.warn("Attribute deletion rejected storeId={} publicId={} reason=in_use", storeId, publicId);
            throw new AttributeInUseException(FIELD_CODE,
                    "Attribute is used by products and cannot be deleted");
        }
        List<Long> affectedCategories = bindingRepository.findByAttributeId(entity.getId()).stream()
                .map(CategoryAttributeEntity::getCategoryId)
                .distinct()
                .toList();

        attributeRepository.delete(entity);
        attributeRepository.flush();
        affectedCategories.forEach(this::resequenceBindings);
        log.info("Attribute deleted storeId={} publicId={}", storeId, publicId);
    }

    // ---------------------------------------------------------------- options

    @Override
    @Transactional
    public AttributeOptionView addOption(Long storeId, String attributePublicId, AttributeOption option) {
        AttributeEntity attribute = requireOptionBased(storeId, attributePublicId);

        AttributeOptionEntity entity = new AttributeOptionEntity();
        entity.setAttributeId(attribute.getId());
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ATTRIBUTE_OPTION_PREFIX));
        entity.setCode(requireValidCode(option.code(), "options.code"));
        entity.setName(requireText(option.name(), "options.name", "Option name is required"));
        entity.setSortOrder(optionRepository.countByAttributeId(attribute.getId()));

        AttributeOptionEntity saved;
        try {
            saved = optionRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException duplicate) {
            throw new AttributeAlreadyExistsException("options.code",
                    "Option with this code already exists for the attribute");
        }
        log.info("Attribute option added storeId={} attributeId={} optionId={} code={}",
                storeId, attribute.getId(), saved.getId(), saved.getCode());
        return attributeOptionEntityMapper.toView(saved);
    }

    @Override
    @Transactional
    public List<AttributeOptionView> updateOption(Long storeId, String attributePublicId, String optionPublicId,
                                                  String name, Integer sortOrder) {
        AttributeEntity attribute = requireOptionBased(storeId, attributePublicId);
        AttributeOptionEntity option = optionRepository
                .findByAttributeIdAndPublicId(attribute.getId(), optionPublicId)
                .orElseThrow(() -> new NotFoundException("Attribute option not found"));

        if (StringUtils.hasText(name)) {
            option.setName(name);
        }
        List<AttributeOptionEntity> options =
                optionRepository.findByAttributeIdOrderBySortOrderAsc(attribute.getId());
        if (sortOrder != null) {
            options = applyOptionOrder(options, optionList(options).move(option.getId(), sortOrder));
        } else {
            optionRepository.save(option);
        }
        log.info("Attribute option updated storeId={} attributeId={} optionId={}",
                storeId, attribute.getId(), option.getId());
        return attributeOptionEntityMapper.toViews(options);
    }

    @Override
    @Transactional
    public void deleteOption(Long storeId, String attributePublicId, String optionPublicId) {
        AttributeEntity attribute = requireOptionBased(storeId, attributePublicId);
        AttributeOptionEntity option = optionRepository
                .findByAttributeIdAndPublicId(attribute.getId(), optionPublicId)
                .orElseThrow(() -> new NotFoundException("Attribute option not found"));
        if (valueRepository.existsByOptionId(option.getId())) {
            log.warn("Attribute option deletion rejected storeId={} optionId={} reason=in_use",
                    storeId, option.getId());
            throw new AttributeInUseException("options.code",
                    "Option is selected by products and cannot be deleted");
        }

        List<AttributeOptionEntity> options =
                optionRepository.findByAttributeIdOrderBySortOrderAsc(attribute.getId());
        optionRepository.delete(option);
        optionRepository.flush();
        applyOptionOrder(options.stream().filter(candidate -> !candidate.getId().equals(option.getId())).toList(),
                optionList(options).without(option.getId()));
        log.info("Attribute option deleted storeId={} attributeId={} optionId={}",
                storeId, attribute.getId(), option.getId());
    }

    @Override
    public List<String> suggestValues(Long storeId, String attributePublicId, String search) {
        AttributeEntity attribute = requireOwned(storeId, attributePublicId);
        if (attribute.getValueType() != AttributeValueType.TEXT) {
            return List.of();
        }
        String like = "%" + (search == null ? "" : search.toLowerCase()) + "%";
        return valueRepository.findDistinctValueStrings(attribute.getId(), like,
                PageRequest.of(0, SUGGESTION_LIMIT));
    }

    // ------------------------------------------------------- category binding

    @Override
    public List<CategoryAttributeView> listCategoryAttributes(Long storeId, String categoryPublicId) {
        return effectiveForPath(storeId, categoriesApi.findAncestorPathByPublicId(storeId, categoryPublicId));
    }

    @Override
    @Transactional
    public List<CategoryAttributeView> bindToCategory(Long storeId, String categoryPublicId,
                                                      List<String> attributePublicIds, List<String> templateCodes) {
        List<CategoryPathEntry> path = categoriesApi.findAncestorPathByPublicId(storeId, categoryPublicId);
        Long categoryId = path.getFirst().id();

        List<AttributeEntity> requested = new ArrayList<>(adoptTemplates(storeId, templateCodes));
        requested.addAll(resolveAttributes(storeId, attributePublicIds));

        // Also skip what an ancestor already offers: a second binding would only shadow the first one.
        Set<Long> alreadyOffered = bindingRepository
                .findByCategoryIdInOrderBySortOrderAsc(path.stream().map(CategoryPathEntry::id).toList()).stream()
                .map(CategoryAttributeEntity::getAttributeId)
                .collect(Collectors.toSet());

        int position = bindingRepository.countByCategoryId(categoryId);
        List<CategoryAttributeEntity> created = new ArrayList<>();
        for (AttributeEntity attribute : dedupeById(requested)) {
            if (attribute.getScope() == AttributeScope.GLOBAL) {
                throw new InvalidAttributeRequestException(FIELD_ATTRIBUTE_IDS,
                        "Attribute '" + attribute.getCode() + "' is global and already reaches every product");
            }
            if (!alreadyOffered.add(attribute.getId())) {
                continue;
            }
            CategoryAttributeEntity binding = new CategoryAttributeEntity();
            binding.setCategoryId(categoryId);
            binding.setAttributeId(attribute.getId());
            binding.setSortOrder(position++);
            created.add(binding);
        }
        if (!created.isEmpty()) {
            bindingRepository.saveAll(created);
            log.info("Category attributes bound storeId={} categoryId={} bound={}",
                    storeId, categoryId, created.size());
        }
        return effectiveForPath(storeId, path);
    }

    @Override
    @Transactional
    public List<CategoryAttributeView> moveBinding(Long storeId, String categoryPublicId,
                                                   String attributePublicId, int sortOrder) {
        List<CategoryPathEntry> path = categoriesApi.findAncestorPathByPublicId(storeId, categoryPublicId);
        Long categoryId = path.getFirst().id();
        AttributeEntity attribute = requireOwned(storeId, attributePublicId);
        requireOwnBinding(categoryId, attribute, path);

        List<CategoryAttributeEntity> bindings = bindingRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);
        List<Long> order = AttributeOrder
                .of(bindings.stream().map(CategoryAttributeEntity::getId).toList())
                .move(bindingOf(bindings, attribute.getId()).getId(), sortOrder);
        applyBindingOrder(bindings, order);

        log.info("Category attribute moved storeId={} categoryId={} attributeId={} position={}",
                storeId, categoryId, attribute.getId(), sortOrder);
        return effectiveForPath(storeId, path);
    }

    @Override
    @Transactional
    public void unbindFromCategory(Long storeId, String categoryPublicId, String attributePublicId) {
        List<CategoryPathEntry> path = categoriesApi.findAncestorPathByPublicId(storeId, categoryPublicId);
        Long categoryId = path.getFirst().id();
        AttributeEntity attribute = requireOwned(storeId, attributePublicId);
        requireOwnBinding(categoryId, attribute, path);

        CategoryAttributeEntity binding = bindingRepository
                .findByCategoryIdAndAttributeId(categoryId, attribute.getId())
                .orElseThrow(() -> new NotFoundException("Attribute is not bound to this category"));
        bindingRepository.delete(binding);
        bindingRepository.flush();
        resequenceBindings(categoryId);
        log.info("Category attribute unbound storeId={} categoryId={} attributeId={}",
                storeId, categoryId, attribute.getId());
    }

    // ---------------------------------------------------------- product values

    @Override
    public List<ProductAttributeValueView> listProductValues(Long storeId, Long productId, Long categoryId) {
        List<ProductAttributeValueEntity> rows =
                valueRepository.findByProductIdOrderByAttributeIdAscSortOrderAsc(productId);
        // Resolving what the category offers costs a category-tree read, so skip it for a bare product.
        return rows.isEmpty() ? List.of() : readValues(storeId, rows, offeredByPublicId(storeId, categoryId));
    }

    @Override
    public ProductAttributesView describeProductAttributes(Long storeId, Long productId, Long categoryId) {
        List<CategoryAttributeView> offered =
                effectiveForPath(storeId, categoriesApi.findAncestorPath(storeId, categoryId));
        Map<String, AttributeEntity> offeredByPublicId = offeredByPublicId(storeId, categoryId);
        return new ProductAttributesView(offered, readValues(storeId, productId, offeredByPublicId));
    }

    @Override
    @Transactional
    public List<ProductAttributeValueView> replaceProductValues(Long storeId, Long productId, Long categoryId,
                                                                List<AttributeValue> values) {
        Map<String, AttributeEntity> offered = offeredByPublicId(storeId, categoryId);
        List<ProductAttributeValueEntity> rows = buildRows(productId, offered, values);

        valueRepository.deleteAll(valueRepository.findByProductIdOrderByAttributeIdAscSortOrderAsc(productId));
        valueRepository.flush();
        valueRepository.saveAll(rows);

        log.info("Product attribute values replaced storeId={} productId={} attributes={}",
                storeId, productId, values == null ? 0 : values.size());
        return readValues(storeId, productId, offered);
    }

    @Override
    @Transactional
    public List<ProductAttributeValueView> mergeProductValues(Long storeId, Long productId, Long categoryId,
                                                              List<AttributeValue> values) {
        if (values == null || values.isEmpty()) {
            return listProductValues(storeId, productId, categoryId);
        }
        Map<String, AttributeEntity> offered = offeredByPublicId(storeId, categoryId);
        List<ProductAttributeValueEntity> rows = buildRows(productId, offered, values);

        Set<Long> touched = rows.stream()
                .map(ProductAttributeValueEntity::getAttributeId)
                .collect(Collectors.toSet());
        valueRepository.deleteByProductIdAndAttributeIdIn(productId, touched);
        valueRepository.flush();
        valueRepository.saveAll(rows);

        log.info("Product attribute values merged storeId={} productId={} attributes={}",
                storeId, productId, touched.size());
        return readValues(storeId, productId, offered);
    }

    @Override
    public List<AttributeValue> exportProductValues(Long storeId, Long productId) {
        List<ProductAttributeValueEntity> rows =
                valueRepository.findByProductIdOrderByAttributeIdAscSortOrderAsc(productId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, AttributeEntity> attributes = attributesById(storeId, attributeIdsOf(rows));
        Map<Long, AttributeOptionEntity> options = optionsById(attributes.keySet());

        List<AttributeValue> exported = new ArrayList<>();
        for (Map.Entry<Long, List<ProductAttributeValueEntity>> entry : groupByAttribute(rows).entrySet()) {
            AttributeEntity attribute = attributes.get(entry.getKey());
            if (attribute == null) {
                continue;
            }
            exported.add(toAttributeValue(attribute, entry.getValue(), options));
        }
        return List.copyOf(exported);
    }

    // ----------------------------------------------------------------- shared

    private List<CategoryAttributeView> effectiveForPath(Long storeId, List<CategoryPathEntry> path) {
        List<Long> pathIds = path.stream().map(CategoryPathEntry::id).toList();
        List<CategoryAttributeEntity> bindings = pathIds.isEmpty()
                ? List.of()
                : bindingRepository.findByCategoryIdInOrderBySortOrderAsc(pathIds);

        Map<Long, AttributeEntity> boundAttributes = attributesById(storeId,
                bindings.stream().map(CategoryAttributeEntity::getAttributeId).collect(Collectors.toSet()));
        List<AttributeEntity> globals = attributeRepository.findByStoreIdAndScope(storeId, AttributeScope.GLOBAL);

        Map<Long, AttributeEntity> byId = new LinkedHashMap<>(boundAttributes);
        globals.forEach(attribute -> byId.put(attribute.getId(), attribute));

        List<AttributeInheritance.EffectiveAttribute> effective = AttributeInheritance.effective(
                pathIds,
                globals.stream().map(attributeEntityMapper::toDomain).toList(),
                bindings.stream()
                        .map(binding -> new AttributeInheritance.Binding(
                                binding.getCategoryId(), binding.getAttributeId(), binding.getSortOrder()))
                        .toList(),
                byId.values().stream()
                        .collect(Collectors.toMap(AttributeEntity::getId, attributeEntityMapper::toDomain)));

        Map<Long, List<AttributeOptionView>> optionsByAttribute = optionViewsByAttribute(byId.keySet());
        Map<Long, CategoryPathEntry> categoriesById = path.stream()
                .collect(Collectors.toMap(CategoryPathEntry::id, Function.identity()));
        Map<String, Integer> positions = bindings.stream().collect(Collectors.toMap(
                binding -> binding.getCategoryId() + ":" + binding.getAttributeId(),
                CategoryAttributeEntity::getSortOrder, (first, second) -> first));

        return effective.stream()
                .map(entry -> {
                    AttributeEntity entity = byId.get(entry.attribute().id());
                    CategoryPathEntry sourceCategory = entry.sourceCategoryId() == null
                            ? null : categoriesById.get(entry.sourceCategoryId());
                    return new CategoryAttributeView(
                            attributeEntityMapper.toView(entity,
                                    optionsByAttribute.getOrDefault(entity.getId(), List.of())),
                            entry.source(),
                            sourceCategory == null ? null : sourceCategory.publicId(),
                            sourceCategory == null ? null : sourceCategory.name(),
                            positions.get(entry.sourceCategoryId() + ":" + entry.attribute().id()));
                })
                .toList();
    }

    /** Definitions a product of this category may take values for, keyed by their public id. */
    private Map<String, AttributeEntity> offeredByPublicId(Long storeId, Long categoryId) {
        List<Long> pathIds = categoriesApi.findAncestorPath(storeId, categoryId).stream()
                .map(CategoryPathEntry::id)
                .toList();
        Map<String, AttributeEntity> offered = new LinkedHashMap<>();
        attributeRepository.findByStoreIdAndScope(storeId, AttributeScope.GLOBAL)
                .forEach(attribute -> offered.put(attribute.getPublicId(), attribute));
        if (!pathIds.isEmpty()) {
            Set<Long> boundIds = bindingRepository.findByCategoryIdInOrderBySortOrderAsc(pathIds).stream()
                    .map(CategoryAttributeEntity::getAttributeId)
                    .collect(Collectors.toSet());
            attributesById(storeId, boundIds).values()
                    .forEach(attribute -> offered.put(attribute.getPublicId(), attribute));
        }
        return offered;
    }

    private List<ProductAttributeValueEntity> buildRows(Long productId, Map<String, AttributeEntity> offered,
                                                        List<AttributeValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Map<Long, Map<String, AttributeOptionEntity>> optionsByAttribute = optionsByAttributeAndPublicId(
                offered.values().stream().map(AttributeEntity::getId).collect(Collectors.toSet()));

        List<ProductAttributeValueEntity> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (AttributeValue value : values) {
            AttributeEntity definition = offered.get(value.attributePublicId());
            if (definition == null) {
                throw new InvalidAttributeRequestException(FIELD_ATTRIBUTES,
                        "Attribute '" + value.attributePublicId() + "' is not available for this product's category");
            }
            if (!seen.add(value.attributePublicId())) {
                throw new InvalidAttributeRequestException(AttributeValuePolicy.fieldOf(
                        attributeEntityMapper.toDomain(definition)), "Attribute is submitted more than once");
            }
            Map<String, AttributeOptionEntity> options =
                    optionsByAttribute.getOrDefault(definition.getId(), Map.of());
            AttributeValuePolicy.validate(attributeEntityMapper.toDomain(definition), options.keySet(), value);
            requireActiveUnit(definition, value);
            rows.addAll(toRows(productId, definition, value, options));
        }
        return rows;
    }

    private List<ProductAttributeValueEntity> toRows(Long productId, AttributeEntity definition,
                                                     AttributeValue value,
                                                     Map<String, AttributeOptionEntity> options) {
        if (definition.getValueType().isOptionBased()) {
            List<ProductAttributeValueEntity> rows = new ArrayList<>();
            List<String> optionIds = value.optionIds();
            for (int position = 0; position < optionIds.size(); position++) {
                ProductAttributeValueEntity row = newRow(productId, definition, position);
                row.setOptionId(options.get(optionIds.get(position)).getId());
                rows.add(row);
            }
            return rows;
        }
        ProductAttributeValueEntity row = newRow(productId, definition, 0);
        row.setValueString(value.valueString());
        row.setValueNumber(value.valueNumber());
        row.setValueBoolean(value.valueBoolean());
        row.setValueDate(value.valueDate());
        row.setUnitCode(value.unitCode() != null ? value.unitCode() : definition.getUnitDefaultCode());
        return List.of(row);
    }

    private static ProductAttributeValueEntity newRow(Long productId, AttributeEntity definition, int position) {
        ProductAttributeValueEntity row = new ProductAttributeValueEntity();
        row.setProductId(productId);
        row.setAttributeId(definition.getId());
        row.setSortOrder(position);
        return row;
    }

    private List<ProductAttributeValueView> readValues(Long storeId, Long productId,
                                                       Map<String, AttributeEntity> offered) {
        return readValues(storeId, valueRepository.findByProductIdOrderByAttributeIdAscSortOrderAsc(productId),
                offered);
    }

    private List<ProductAttributeValueView> readValues(Long storeId, List<ProductAttributeValueEntity> rows,
                                                       Map<String, AttributeEntity> offered) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, AttributeEntity> attributes = attributesById(storeId, attributeIdsOf(rows));
        Map<Long, AttributeOptionEntity> options = optionsById(attributes.keySet());
        Set<Long> offeredIds = offered.values().stream()
                .map(AttributeEntity::getId)
                .collect(Collectors.toSet());

        return groupByAttribute(rows).entrySet().stream()
                .map(entry -> {
                    AttributeEntity attribute = attributes.get(entry.getKey());
                    if (attribute == null) {
                        return null;
                    }
                    ProductAttributeValueEntity first = entry.getValue().getFirst();
                    return new ProductAttributeValueView(
                            attribute.getPublicId(),
                            attribute.getCode(),
                            attribute.getName(),
                            attribute.getValueType(),
                            attribute.getVariantDefining(),
                            offeredIds.contains(attribute.getId()),
                            first.getValueString(),
                            first.getValueNumber(),
                            first.getValueBoolean(),
                            first.getValueDate(),
                            first.getUnitCode(),
                            entry.getValue().stream()
                                    .map(ProductAttributeValueEntity::getOptionId)
                                    .filter(Objects::nonNull)
                                    .map(options::get)
                                    .filter(Objects::nonNull)
                                    .map(attributeOptionEntityMapper::toView)
                                    .toList());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private AttributeValue toAttributeValue(AttributeEntity attribute, List<ProductAttributeValueEntity> rows,
                                            Map<Long, AttributeOptionEntity> options) {
        ProductAttributeValueEntity first = rows.getFirst();
        List<String> optionIds = rows.stream()
                .map(ProductAttributeValueEntity::getOptionId)
                .filter(Objects::nonNull)
                .map(options::get)
                .filter(Objects::nonNull)
                .map(AttributeOptionEntity::getPublicId)
                .toList();
        return new AttributeValue(attribute.getPublicId(), first.getValueString(), first.getValueNumber(),
                first.getValueBoolean(), first.getValueDate(), first.getUnitCode(), optionIds);
    }

    // --------------------------------------------------------------- plumbing

    private List<AttributeEntity> adoptTemplates(Long storeId, List<String> templateCodes) {
        if (templateCodes == null || templateCodes.isEmpty()) {
            return List.of();
        }
        List<AttributeTemplateEntity> templates =
                templateRepository.findByActiveTrueAndCodeInOrderBySortOrderAscCodeAsc(templateCodes);
        Set<String> found = templates.stream().map(AttributeTemplateEntity::getCode).collect(Collectors.toSet());
        templateCodes.stream()
                .filter(code -> !found.contains(code))
                .findFirst()
                .ifPresent(code -> {
                    throw new InvalidAttributeRequestException(FIELD_TEMPLATE_CODES,
                            "Unknown attribute template: " + code);
                });

        Map<String, AttributeEntity> owned = attributeRepository
                .findByStoreIdAndTemplateCodeIn(storeId, found).stream()
                .collect(Collectors.toMap(AttributeEntity::getTemplateCode, Function.identity(),
                        (first, second) -> first));

        Map<Long, List<AttributeTemplateOptionEntity>> templateOptions = templateOptionRepository
                .findByTemplateIdInOrderBySortOrderAsc(templates.stream().map(AttributeTemplateEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(AttributeTemplateOptionEntity::getTemplateId));

        List<AttributeEntity> adopted = new ArrayList<>();
        for (AttributeTemplateEntity template : templates) {
            AttributeEntity existing = owned.get(template.getCode());
            if (existing != null) {
                adopted.add(existing);
                continue;
            }
            AttributeEntity entity = attributeEntityMapper.fromTemplate(template);
            entity.setStoreId(storeId);
            entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ATTRIBUTE_PREFIX));
            entity.setScope(AttributeScope.CATEGORY);
            entity.setStatus(AttributeStatus.ACTIVE);

            AttributeEntity saved = save(entity, FIELD_TEMPLATE_CODES);
            copyTemplateOptions(saved, templateOptions.getOrDefault(template.getId(), List.of()));
            adopted.add(saved);
            log.info("Attribute adopted from template storeId={} attributeId={} templateCode={}",
                    storeId, saved.getId(), template.getCode());
        }
        return adopted;
    }

    private void copyTemplateOptions(AttributeEntity attribute, List<AttributeTemplateOptionEntity> options) {
        if (options.isEmpty()) {
            return;
        }
        List<AttributeOptionEntity> copies = new ArrayList<>();
        for (int position = 0; position < options.size(); position++) {
            AttributeTemplateOptionEntity source = options.get(position);
            AttributeOptionEntity copy = new AttributeOptionEntity();
            copy.setAttributeId(attribute.getId());
            copy.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ATTRIBUTE_OPTION_PREFIX));
            copy.setCode(source.getCode());
            copy.setName(source.getName());
            copy.setSortOrder(position);
            copies.add(copy);
        }
        optionRepository.saveAll(copies);
    }

    private List<AttributeEntity> resolveAttributes(Long storeId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return List.of();
        }
        List<AttributeEntity> found = attributeRepository.findByStoreIdAndPublicIdIn(storeId, publicIds);
        if (found.size() != new LinkedHashSet<>(publicIds).size()) {
            Set<String> resolved = found.stream().map(AttributeEntity::getPublicId).collect(Collectors.toSet());
            String missing = publicIds.stream().filter(id -> !resolved.contains(id)).findFirst().orElse("");
            throw new NotFoundException("Attribute not found: " + missing);
        }
        return found;
    }

    private List<AttributeOptionEntity> replaceOptions(AttributeEntity attribute, List<AttributeOption> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        if (!attribute.getValueType().isOptionBased()) {
            throw new InvalidAttributeRequestException("options",
                    "Attribute of type " + attribute.getValueType() + " does not take options");
        }
        List<AttributeOptionEntity> entities = new ArrayList<>();
        for (int position = 0; position < options.size(); position++) {
            AttributeOption option = options.get(position);
            AttributeOptionEntity entity = new AttributeOptionEntity();
            entity.setAttributeId(attribute.getId());
            entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ATTRIBUTE_OPTION_PREFIX));
            entity.setCode(requireValidCode(option.code(), "options.code"));
            entity.setName(requireText(option.name(), "options.name", "Option name is required"));
            entity.setSortOrder(position);
            entities.add(entity);
        }
        try {
            return optionRepository.saveAllAndFlush(entities);
        } catch (DataIntegrityViolationException duplicate) {
            throw new AttributeAlreadyExistsException("options.code",
                    "Option codes must be unique within the attribute");
        }
    }

    private List<AttributeOptionEntity> applyOptionOrder(List<AttributeOptionEntity> options, List<Long> order) {
        Map<Long, AttributeOptionEntity> byId = options.stream()
                .collect(Collectors.toMap(AttributeOptionEntity::getId, Function.identity()));
        List<AttributeOptionEntity> ordered = order.stream().map(byId::get).filter(Objects::nonNull).toList();
        for (int position = 0; position < ordered.size(); position++) {
            ordered.get(position).setSortOrder(position);
        }
        optionRepository.saveAll(ordered);
        return ordered;
    }

    private void applyBindingOrder(List<CategoryAttributeEntity> bindings, List<Long> order) {
        Map<Long, CategoryAttributeEntity> byId = bindings.stream()
                .collect(Collectors.toMap(CategoryAttributeEntity::getId, Function.identity()));
        List<CategoryAttributeEntity> ordered = order.stream().map(byId::get).filter(Objects::nonNull).toList();
        for (int position = 0; position < ordered.size(); position++) {
            ordered.get(position).setSortOrder(position);
        }
        bindingRepository.saveAll(ordered);
    }

    private void resequenceBindings(Long categoryId) {
        List<CategoryAttributeEntity> bindings = bindingRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);
        applyBindingOrder(bindings, bindings.stream().map(CategoryAttributeEntity::getId).toList());
    }

    private void requireOwnBinding(Long categoryId, AttributeEntity attribute, List<CategoryPathEntry> path) {
        if (bindingRepository.findByCategoryIdAndAttributeId(categoryId, attribute.getId()).isPresent()) {
            return;
        }
        List<Long> ancestorIds = path.stream().skip(1).map(CategoryPathEntry::id).toList();
        if (!ancestorIds.isEmpty()) {
            bindingRepository.findByCategoryIdInOrderBySortOrderAsc(ancestorIds).stream()
                    .filter(binding -> binding.getAttributeId().equals(attribute.getId()))
                    .findFirst()
                    .ifPresent(binding -> {
                        throw new InvalidAttributeRequestException(FIELD_ATTRIBUTE_IDS,
                                "Attribute '" + attribute.getCode() + "' is inherited from a parent category "
                                        + "and must be changed there");
                    });
        }
        throw new NotFoundException("Attribute is not bound to this category");
    }

    private static CategoryAttributeEntity bindingOf(List<CategoryAttributeEntity> bindings, Long attributeId) {
        return bindings.stream()
                .filter(binding -> binding.getAttributeId().equals(attributeId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Attribute is not bound to this category"));
    }

    private AttributeEntity save(AttributeEntity entity, String field) {
        try {
            return attributeRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException duplicate) {
            throw new AttributeAlreadyExistsException(field,
                    "Attribute with code '" + entity.getCode() + "' already exists in the store");
        }
    }

    private AttributeEntity requireOwned(Long storeId, String publicId) {
        return attributeRepository.findByStoreIdAndPublicId(storeId, publicId)
                .orElseThrow(() -> new NotFoundException("Attribute not found"));
    }

    private AttributeEntity requireOptionBased(Long storeId, String publicId) {
        AttributeEntity attribute = requireOwned(storeId, publicId);
        if (!attribute.getValueType().isOptionBased()) {
            throw new InvalidAttributeRequestException("value_type",
                    "Attribute of type " + attribute.getValueType() + " does not take options");
        }
        return attribute;
    }

    private void requireKnownUnit(AttributeEntity entity) {
        if (entity.getUnitDefaultCode() == null) {
            return;
        }
        if (entity.getUnitDictionaryCode() == null) {
            throw new InvalidAttributeRequestException("unit_default_code",
                    "A default unit needs a unit dictionary");
        }
        if (!dictionariesApi.isActiveItem(entity.getUnitDictionaryCode(), entity.getUnitDefaultCode())) {
            throw new InvalidAttributeRequestException("unit_default_code",
                    "Unknown unit '" + entity.getUnitDefaultCode() + "' in dictionary '"
                            + entity.getUnitDictionaryCode() + "'");
        }
    }

    private void requireActiveUnit(AttributeEntity definition, AttributeValue value) {
        if (definition.getUnitDictionaryCode() == null || value.unitCode() == null) {
            return;
        }
        if (!dictionariesApi.isActiveItem(definition.getUnitDictionaryCode(), value.unitCode())) {
            throw new InvalidAttributeRequestException(
                    AttributeValuePolicy.fieldOf(attributeEntityMapper.toDomain(definition)),
                    "Unknown unit '" + value.unitCode() + "' in dictionary '"
                            + definition.getUnitDictionaryCode() + "'");
        }
    }

    private static void applyDefaults(AttributeEntity entity) {
        if (entity.getScope() == null) {
            entity.setScope(AttributeScope.CATEGORY);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(AttributeStatus.ACTIVE);
        }
        if (entity.getVariantDefining() == null) {
            entity.setVariantDefining(Boolean.FALSE);
        }
    }

    private static void requireValueType(AttributeEntity entity) {
        if (entity.getValueType() == null) {
            throw new InvalidAttributeRequestException("value_type", "Attribute value type is required");
        }
    }

    private static String requireValidCode(String code, String field) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (!ValidationPatterns.RESOURCE_CODE.matcher(normalized).matches()) {
            throw new InvalidAttributeRequestException(field,
                    "Code must start with a letter and use only A-Z, 0-9 and underscore");
        }
        return normalized;
    }

    private static String requireText(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidAttributeRequestException(field, message);
        }
        return value;
    }

    private static boolean matchesSearch(AttributeTemplateEntity template, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String needle = search.toLowerCase();
        return template.getCode().toLowerCase().contains(needle)
                || template.getName().toLowerCase().contains(needle);
    }

    private static List<AttributeEntity> dedupeById(List<AttributeEntity> attributes) {
        Map<Long, AttributeEntity> byId = new LinkedHashMap<>();
        attributes.forEach(attribute -> byId.putIfAbsent(attribute.getId(), attribute));
        return List.copyOf(byId.values());
    }

    private static AttributeOrder optionList(List<AttributeOptionEntity> options) {
        return AttributeOrder.of(options.stream().map(AttributeOptionEntity::getId).toList());
    }

    private static Set<Long> attributeIdsOf(List<ProductAttributeValueEntity> rows) {
        return rows.stream().map(ProductAttributeValueEntity::getAttributeId).collect(Collectors.toSet());
    }

    private static Map<Long, List<ProductAttributeValueEntity>> groupByAttribute(
            List<ProductAttributeValueEntity> rows) {
        return rows.stream().collect(Collectors.groupingBy(ProductAttributeValueEntity::getAttributeId,
                LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, AttributeEntity> attributesById(Long storeId, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return attributeRepository.findByStoreIdAndIdIn(storeId, ids).stream()
                .collect(Collectors.toMap(AttributeEntity::getId, Function.identity()));
    }

    private Map<Long, AttributeOptionEntity> optionsById(Collection<Long> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByAttributeIdInOrderBySortOrderAsc(attributeIds).stream()
                .collect(Collectors.toMap(AttributeOptionEntity::getId, Function.identity()));
    }

    private Map<Long, Map<String, AttributeOptionEntity>> optionsByAttributeAndPublicId(Collection<Long> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByAttributeIdInOrderBySortOrderAsc(attributeIds).stream()
                .collect(Collectors.groupingBy(AttributeOptionEntity::getAttributeId,
                        Collectors.toMap(AttributeOptionEntity::getPublicId, Function.identity())));
    }

    private Map<Long, List<AttributeOptionView>> optionViewsByAttribute(Collection<Long> attributeIds) {
        if (attributeIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByAttributeIdInOrderBySortOrderAsc(attributeIds).stream()
                .collect(Collectors.groupingBy(AttributeOptionEntity::getAttributeId, LinkedHashMap::new,
                        Collectors.mapping(attributeOptionEntityMapper::toView, Collectors.toList())));
    }

    private List<AttributeView> toViews(List<AttributeEntity> entities) {
        Map<Long, List<AttributeOptionView>> options = optionViewsByAttribute(
                entities.stream().map(AttributeEntity::getId).toList());
        return entities.stream()
                .map(entity -> attributeEntityMapper.toView(entity,
                        options.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private AttributeView toView(AttributeEntity entity) {
        return attributeEntityMapper.toView(entity,
                attributeOptionEntityMapper.toViews(
                        optionRepository.findByAttributeIdOrderBySortOrderAsc(entity.getId())));
    }
}
