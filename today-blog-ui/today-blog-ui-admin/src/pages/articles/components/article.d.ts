export interface Post {
  title: string
  content: string
  markdown: string
  image: string | undefined
}

export interface PostCategory {
  name: string
  order: number
  description: string
  articleCount: number
}

export interface PostLabel {
  id: number
  name: string
}
