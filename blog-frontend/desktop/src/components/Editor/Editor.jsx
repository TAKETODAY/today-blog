import * as React from "react"
import "./markdown.css"
import "../../assets/css/github.css"

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
  setElementWrapperRef = (element) => { }

  keyChange = false

  state = {
    value: this.props.value || ""
  }

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
    const markdown = require("./markdown")
    const initialOptions = {
      element: document.getElementById(this.id),
      autosave: {
        useSession: true
      },
      autofocus: true,
      renderingConfig: {

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

    this.markdown = new markdown(Object.assign(initialOptions, this.props.options))
  }

  eventWrapper = () => {
    this.keyChange = true
    const value = this.markdown.value()
    this.setState({ value })

    this.props.onChange && this.props.onChange(value)
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
