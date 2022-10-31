import JoditEditor from "jodit-react";
import React from "react";

const config = {
  readonly: false // all options from https://xdsoft.net/jodit/doc/
}

export default (props) => {
  const { post, saveContent } = props

  const handleEditorChange = (content) => {
    saveContent(content)
  }

  return (
      <JoditEditor
          value={ post.content }
          config={ config }
          tabIndex={ 1 }
          onChange={ handleEditorChange }
      />
  )
}
