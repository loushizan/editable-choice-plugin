/*
 * The MIT License
 *
 * Copyright (c) 2012-2013 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.editable-choice-suggest').forEach((e) => {
    const textbox = e.querySelector('.editable-choice-suggest-input-block input[type="text"]');
    const choiceContainer = e.querySelector('.editable-choice-suggest-choices');
    const choices = choiceContainer.querySelectorAll('[data-value]');
    const choiceState = {
      'activeIdx': null
    };
    const setActive = (idx) => {
      // set active choice
      if (choiceState.activeIdx != null) {
        if (choiceState.activeIdx === idx) {
          return;
        }
        choices[choiceState.activeIdx].classList.remove('active');
      }
      choiceState.activeIdx = idx;
      if (choiceState.activeIdx != null) {
        choices[choiceState.activeIdx].classList.add('active');
      }
    }
    const setUpperActive = () => {
      if (choiceState.activeIdx == null || choiceState.activeIdx <= 0) {
        setActive(choices.length - 1);
        return;
      }
      setActive(choiceState.activeIdx - 1);
    }
    const setLowerActive = () => {
      if (choiceState.activeIdx == null || choiceState.activeIdx >= choices.length - 1) {
        setActive(0);
        return;
      }
      setActive(choiceState.activeIdx + 1);
    }

    // set up choices behavior
    // * activate on mouse over
    // * enter value when clicking
    choices.forEach((e, idx) => {
      e.addEventListener('mouseenter', () => {
        setActive(idx);
      });
      e.addEventListener('click', (evt) => {
        evt.stopPropagation();
        textbox.value = e.dataset.value;
      })
    });

    // set up textbox behavir
    // * focus / blur: toggle display of choices
    // * inputting texts: filter values
    // * pressing cursor keys: move active choices
    // * pressing enter: input the value
    textbox.addEventListener('focus', () => {
      choiceContainer.classList.add('active');
    });
    textbox.addEventListener('blur', () => {
      // hiding the block immediately prevents 'click' for choices from firing.
      setTimeout(
        () => {
          choiceContainer.classList.remove('active');
        },
        100
      );
    });
    textbox.addEventListener('keydown', (evt) => {
      switch (evt.keyCode) {
      case 38: // up
        setUpperActive();
        break;
      case 40: // down
        setLowerActive();
        break;
      case 13: // enter
        if (choiceState.activeIdx != null) {
          const newValue = choices[choiceState.activeIdx].dataset.value;
          if (newValue !== textbox.value) {
            textbox.value = newValue;
            // prevent form submit
            evt.stopPropagation();
            evt.preventDefault();
          }
        }
        break;
      }
    });
  });
});
