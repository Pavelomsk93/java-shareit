package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.PageRequestOverride;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BookingException;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.shareit.booking.mapper.BookingMapper.toBookingDto;
import static ru.practicum.shareit.booking.model.Status.REJECTED;
import static ru.practicum.shareit.booking.model.Status.WAITING;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public List<BookingDto> getAllBookings(Long userId, String stateParam, int from, int size) {
        Status state = Status.from(stateParam);
        if (state == null) {
            log.error("Unknown state: " + stateParam);
            throw new IllegalArgumentException("Unknown state: " + stateParam);
        }
        if (from < 0 || size <= 0) {
            log.error("Переданы некорректные значения from и/или size");
            throw new ValidationException("Переданы некорректные значения from и/или size");
        }

        PageRequestOverride pageRequest = PageRequestOverride.of(from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователь %s не существует.", userId)));
        switch (Status.valueOf(stateParam)) {
            case CURRENT:
                return bookingRepository
                        .findCurrentBookingsByBookerIdOrderByStartDesc(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case PAST:
                return bookingRepository
                        .findBookingsByBookerIdAndEndIsBeforeOrderByStartDesc(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case FUTURE:
                return bookingRepository
                        .findByBookerIdAndStartAfterOrderByStartDesc(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case WAITING:
                return bookingRepository
                        .findBookingsByBookerIdAndStatusOrderByStartDesc(
                                userId,
                                WAITING,
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case REJECTED:
                return bookingRepository
                        .findBookingsByBookerIdAndStatusOrderByStartDesc(
                                userId,
                                REJECTED,
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            default:
                return bookingRepository
                        .findByBookerIdOrderByStartDesc(
                                userId,
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
        }
    }

    @Override
    public BookingDto getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new EntityNotFoundException(
                String.format("Бронирование %s не существует.", bookingId)));
        if (!booking.getBooker().getId().equals(userId) && !booking.getItem().getOwner().getId().equals(userId)) {
            log.error("Пользователь {} не осуществлял бронирование.", userId);
            throw new EntityNotFoundException(String.format("Пользователь %s не осуществлял бронирование.", userId));
        }
        return toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllBookingItemsUser(Long userId, String stateParam, int from, int size) {
        Status state = Status.from(stateParam);
        if (state == null) {
            log.error("Unknown state: " + stateParam);
            throw new IllegalArgumentException("Unknown state: " + stateParam);
        }
        if (from < 0 || size <= 0) {
            log.error("Переданы некорректные значения from и/или size");
            throw new ValidationException("Переданы некорректные значения from и/или size");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователь %s не существует.", userId)));
        PageRequestOverride pageRequest = PageRequestOverride.of(from, size);

        List<BookingDto> bookingsUserList = bookingRepository.searchBookingByItemOwnerIdOrderByStartDesc(
                        userId,
                        pageRequest)
                .stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());

        if (bookingsUserList.isEmpty()) {
            log.error("У пользователя нет вещей.");
            throw new EntityNotFoundException("У пользователя нет вещей.");
        }

        switch (Status.valueOf(stateParam)) {
            case ALL:
                bookingsUserList.sort(Comparator.comparing(BookingDto::getStart).reversed());
                return bookingsUserList;
            case CURRENT:
                return bookingRepository
                        .findCurrentBookingsByItemOwnerIdOrderByStartDesc(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case PAST:
                return bookingRepository
                        .findBookingsByItemOwnerIdAndEndIsBefore(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case FUTURE:
                return bookingRepository
                        .searchBookingByItemOwnerIdAndStartIsAfterOrderByStartDesc(
                                userId,
                                LocalDateTime.now(),
                                pageRequest)
                        .stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case WAITING:
                return bookingRepository
                        .findBookingsByItemOwnerIdOrderByStartDesc(
                                userId,
                                pageRequest)
                        .stream()
                        .filter(booking -> booking.getStatus().equals(WAITING))
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            case REJECTED:
                return bookingRepository
                        .findBookingsByItemOwnerIdOrderByStartDesc(
                                userId,
                                pageRequest)
                        .stream()
                        .filter(booking -> booking.getStatus().equals(REJECTED))
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public BookingDto createBooking(Long userId, BookingCreateDto bookingDto) {
        Booking booking = BookingMapper.toBookingCreate(bookingDto);
        booking.setBooker(userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователь %s не существует.", userId))));
        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Вещь %s не существует.", bookingDto.getItemId())));

        if (item.getOwner().getId().equals(userId)) {
            log.error("Владелец вещи не может забронировать свою вещь.");
            throw new EntityNotFoundException("Владелец вещи не может забронировать свою вещь.");
        }
        if (booking.getEnd().isBefore(booking.getStart())) {
            log.error("Некорректное время окончания бронирования.");
            throw new BookingException("Некорректное время окончания бронирования.");
        }
        if (booking.getStart().isBefore(LocalDateTime.now())) {
            log.error("Некорректное время начала бронирования.");
            throw new BookingException("Некорректное время начала бронирования.");
        }
        if (item.getAvailable()) {
            booking.setItem(item);
            Booking bookingCreate = bookingRepository.save(booking);
            log.info("Создан пользователь: {} ", bookingCreate);
            return BookingMapper.toBookingDto(bookingCreate);
        } else {
            log.error("Вещь {} не доступна для бронирования.", item.getId());
            throw new ValidationException(
                    String.format("Вещь %s не доступна для бронирования.", item.getId()));
        }
    }

    @Override
    @Transactional
    public BookingDto patchBooking(Long userId, Long bookingId, Boolean approved) {
        BookingDto bookingDto = toBookingDto(bookingRepository.findById(bookingId).orElseThrow());
        Booking booking = BookingMapper.toBooking(bookingDto);
        if (!userId.equals(bookingDto.getItem().getOwner().getId())) {
            log.error("Подтвердить бронирование может только владелец вещи.");
            throw new EntityNotFoundException("Подтвердить бронирование может только владелец вещи.");
        }
        if (booking.getStatus().equals(Status.APPROVED)) {
            log.error("Бронирование уже было подтверждено.");
            throw new BookingException("Бронирование уже было подтверждено.");
        }
        if (approved == null) {
            log.error("Необходимо указать статус возможности аренды (approved).");
            throw new BookingException("Необходимо указать статус возможности аренды (approved).");
        } else if (approved) {
            booking.setStatus(Status.APPROVED);
            Booking bookingSave = bookingRepository.save(booking);
            return toBookingDto(bookingSave);
        } else {
            booking.setStatus(REJECTED);
            booking.setItem(bookingDto.getItem());
            Booking bookingSave = bookingRepository.save(booking);
            log.info("Обновлён пользователь {}", bookingSave);
            return toBookingDto(bookingSave);
        }
    }

    @Override
    @Transactional
    public void removeBookingById(Long bookingId) {
        log.info("Вещь {} удалена", bookingId);
        bookingRepository.deleteById(bookingId);
    }
}
