package com.kiwimeaty.apps.meditation.util;

import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * An UnlockList will unlock its elements one by one by calling
 * {@link #unlockNextElement}. The first element is always unlocked, also after
 * resetting the list. This list is unmodifiable.
 * 
 * @param <E> the element type for the unlockList
 */
public final class UnlockList<E> extends AbstractList<E> {

    public enum ElementState {
        /** This element is still locked. */
        LOCKED,
        /** This element is unlocked. */
        UNLOCKED,
        /** This is the latest unlocked element. */
        LATEST_UNLOCKED
    }

    /**
     * Item includes Element and its state.
     */
    private final class Item {
        private final E elem;
        private final ObjectProperty<ElementState> state;

        Item(final E elem) {
            this.elem = elem;
            this.state = new SimpleObjectProperty<>(ElementState.LOCKED);
        }

    }

    private final List<Item> list;

    public UnlockList(final List<E> elements) {
        // unmodifiable list
        this.list = elements.stream().map(Item::new).toList();
        this.list.get(0).state.set(ElementState.LATEST_UNLOCKED);
    }

    public boolean unlockNextElement(final E currentElement) {
        final var currentItem = getItem(currentElement);
        if (currentItem.state.get() == ElementState.UNLOCKED) {
            final var item = getItem(currentElement);
            final var elementIndex = this.list.indexOf(item);
            if (elementIndex < this.list.size() - 1) {
                final var nextItem = this.list.get(elementIndex + 1);
                if (nextItem.state.get() == ElementState.LOCKED) {
                    nextItem.state.set(ElementState.LATEST_UNLOCKED);
                    return true;
                }
            }
        }
        return false;
    }

    public void resetList() {
        this.list.forEach(item -> item.state.set(ElementState.LOCKED));
        this.list.get(0).state.set(ElementState.LATEST_UNLOCKED);
    }

    /**
     * Returns the one element with the {@link ElementState.LATEST_UNLOCKED}. If all
     * have been {@link ElementState.UNLOCKED UNLOCKED} then the last element in the
     * list
     * will be returned.
     * 
     * @return the latest unlocked element
     */
    public E getLatestUnlockedElement() throws IllegalArgumentException {
        return this.list.stream().filter(item -> item.state.get() == ElementState.LATEST_UNLOCKED)//
                .map(item -> item.elem).findFirst()
                .orElse(this.list.get(this.list.size() - 1).elem);
    }

    public int getIndexOfLatestUnlockedElement() {
        return this.list.indexOf(getItem(getLatestUnlockedElement()));
    }

    /**
     * Returns the states of the elements in the list. The states are ordered.
     * 
     * @return the states of the elements in the right order.
     */
    public List<ObjectProperty<UnlockList.ElementState>> getStates() {
        return this.list.stream().map(item -> item.state).toList();
    }

    public ObjectProperty<UnlockList.ElementState> getState(final E element) {
        return getItem(element).state;
    }

    // #############################################################
    private Item getItem(final E currentElement) {
        return this.list.stream().filter(item -> item.elem.equals(currentElement)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("no such element: " + currentElement));
    }

    // ########### unmodifiable list >> see javadoc of AbstractList<E>
    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public E get(final int index) {
        return this.list.get(index).elem;
    }
}