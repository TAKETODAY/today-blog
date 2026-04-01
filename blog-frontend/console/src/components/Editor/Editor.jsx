/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import * as React from "react"
import "./markdown.css"
import "./github.css"

import Markdown from "./markdown"

let _id = 0

const generateId = () => `editor-${++_id}`

// type CodemirrorEvents =
//   | "change"
//   | "changes"
//   | "beforeChange"
//   | "cursorActivity"
//   | "beforeSelectionChange"
//   | "viewportChange"
//   | "gutterClick"
//   | "focus"
//   | "blur"
//   | "scroll"
//   | "update"
//   | "renderLine"

// type SimpleMdeToCodemirror = {
//   [E in CodemirrorEvents | DOMEvent]?: Editor["on"]
// }

// export interface SimpleMDEEditorProps {
//   id?: string
//   label?: string
//   onChange: (value: string) => void | any
//   value?: string
//   className?: string
//   extraKeys?: KeyMap
//   options?: SimpleMDE.Options
//   events?: SimpleMdeToCodemirror
//   getMdeInstance?: (instance: SimpleMDE) => void | any
//   getLineAndCursor?: (position: CodeMirror.Position) => void | any
// }

export default class Editor extends React.PureComponent {
  elementWrapperRef
  setElementWrapperRef = (element) => {
  }

  keyChange = false

  // state = {
  //   value: this.props.value || ""
  // }

  id = this.props.id ? this.props.id : generateId()
  markdown = null
  editorEl = null
  editorToolbarEl = null

  constructor(props) {
    super(props)
    this.elementWrapperRef = null
    this.setElementWrapperRef = (element) => {
      this.elementWrapperRef = element
    }
  }

  componentDidMount() {
    if (typeof window !== undefined) {
      this.createEditor()
      this.addEvents()
      this.addExtraKeys()
      this.getCursor()
      this.setEditor()
    }
  }

  componentDidUpdate(prevProps) {
    // if (!this.keyChange &&
    //   this.props.value !== this.state.value && // This is somehow fixes moving cursor for controlled case
    //   this.props.value !== prevProps.value // This one fixes no value change for uncontrolled input. If it's uncontrolled prevProps will be the same
    // ) {
    //   this.markdown.value(this.props.value || "")
    // }
    this.keyChange = false
  }

  componentWillUnmount() {
    this.removeEvents()
  }

  createEditor = () => {
    const initialOptions = {
      element: document.getElementById(this.id),
      autosave: {
        useSession: true
      },
      autofocus: true,
      renderingConfig: {
        linkify: true,
      },
      initialValue: this.props.value,
      autoDownloadFontAwesome: false,
      status: ["lines", "words"],
      tabSize: 4,
      promptURLs: true,
      toolbar: [
        "bold", "italic", "strikethrough", "heading", "|",
        "code", "quote", "unordered-list", "ordered-list", "|",
        "link", "image", "table", "horizontal-rule", "|",
        "preview", "undo", "redo", "|", "guide", "|"
      ]
    }

    this.markdown = new Markdown(Object.assign(initialOptions, this.props.options))
  }

  eventWrapper = () => {
    this.keyChange = true
    let markdown = this.markdown;
    const value = markdown.value()
    // this.setState({ value })
    const { onChange } = this.props
    onChange && onChange(value, markdown.markdown(value))
  }

  removeEvents = () => {
    if (this.editorEl && this.editorToolbarEl) {
      this.editorEl.removeEventListener("keyup", this.eventWrapper)
      this.editorEl.removeEventListener("paste", this.eventWrapper)
      this.editorToolbarEl.removeEventListener("click", this.eventWrapper)
    }
  }

  addEvents = () => {
    if (this.elementWrapperRef && this.markdown) {
      this.editorEl = this.elementWrapperRef
      this.editorToolbarEl = this.elementWrapperRef.getElementsByClassName("editor-toolbar")[0]

      this.editorEl.addEventListener("keyup", this.eventWrapper)
      this.editorEl.addEventListener("paste", this.eventWrapper)
      this.editorToolbarEl && this.editorToolbarEl.addEventListener("click", this.eventWrapper)

      this.markdown.codemirror.on("cursorActivity", this.getCursor)

      const { events } = this.props

      // Handle custom events
      events && Object.entries(events).forEach(([eventName, callback]) => {
        if (eventName && callback) {
          this.markdown && this.markdown.codemirror.on(
            eventName,
            callback
          )
        }
      })
    }
  }

  getCursor = () => {
    // https://codemirror.net/doc/manual.html#api_selection
    if (this.props.getLineAndCursor) {
      this.props.getLineAndCursor(
        this.markdown.codemirror.getDoc().getCursor()
      )
    }
  }

  setEditor = () => {
    this.props.setEditor && this.props.setEditor(this.markdown)
  }

  addExtraKeys = () => {
    // https://codemirror.net/doc/manual.html#option_extraKeys
    if (this.props.extraKeys) {
      this.markdown.codemirror.setOption("extraKeys", this.props.extraKeys)
    }
  }

  render() {
    const {
      id,
      label,
      value,
      events,
      options,
      setEditor,
      onChange,
      extraKeys,
      getLineAndCursor,
      ...rest
    } = this.props

    return (
      <div id={`${this.id}-wrapper`} ref={this.setElementWrapperRef}>
        {label && <label htmlFor={this.id}> {label} </label>}
        <textarea id={this.id} {...rest} />
      </div>
    )
  }
}
