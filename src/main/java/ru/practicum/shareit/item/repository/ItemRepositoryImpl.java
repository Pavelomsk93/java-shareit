package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.exceptions.EntityNotFoundException;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ItemRepositoryImpl implements ItemRepository{

    private long id = 0;
    private final  Map<Long,Item> items = new HashMap<>();

    @Override
    public List<Item> getAllItem(Long userId) {
        return items.values().stream().filter(f->f.getOwner().getId() == userId).collect(Collectors.toList());
    }

    @Override
    public Item getItemById(Long itemId) {
      return items.values().stream().filter(f-> Objects.equals(f.getId(), itemId)).findAny().orElse(null);
    }

    @Override
    public List<Item> getItemSearch(String text) {
        if(text.isEmpty()){
            return new ArrayList<>();
        }
        return items.values()
                .stream().filter(Item::getAvailable)
                .filter(f->f.getName().toLowerCase().contains(text.toLowerCase())||
                f.getDescription().toLowerCase().contains(text.toLowerCase()))
                .collect(Collectors.toList());

    }

    @Override
    public Item createItem(Item item) {
        item.setId(++id);
        items.put(item.getId(),item);
        return items.get(item.getId());
    }

    @Override
    public void removeItem(Long id) {
        if(!items.containsKey(id)){
            throw new EntityNotFoundException("Такой вещи не существует в списке");
        }
        items.remove(id);

    }

    @Override
    public Item patchItem(Item item, Long itemId) {
        Item updateItem = items.get(itemId);
        updateItem.setName(item.getName());
        updateItem.setDescription(item.getDescription());
        updateItem.setAvailable(item.getAvailable());
        items.put(itemId,updateItem);
        return items.get(itemId);
    }

    @Override
    public Item patchItemAvailable(Item item, Long itemId) {
        Item updateItem = items.get(itemId);
        updateItem.setAvailable(item.getAvailable());
        items.put(itemId,updateItem);
        return items.get(itemId);
    }

    @Override
    public Item patchItemName(Item item, Long itemId) {
        Item updateItem = items.get(itemId);
        updateItem.setName(item.getName());
        items.put(itemId,updateItem);
        return items.get(itemId);
    }

    @Override
    public Item patchItemDescription(Item item, Long itemId) {
        Item updateItem = items.get(itemId);
        updateItem.setDescription(item.getDescription());
        items.put(itemId,updateItem);
        return items.get(itemId);
    }
}