<template>
  <van-field name="uploader" :label="label" label-width="45px" :error-message="error">
    <template #input>
      <van-uploader v-model="images" :max-count="maxCount"
                    accept="image/png, image/jpeg" :after-read="upload" @delete="onDelete"
                    :max-size="1000 * 1024" @oversize="onOversize"/>
    </template>
  </van-field>

</template>

<script>
  import { Toast } from 'vant';
  import { requireLogin } from "src/utils";

  export default {
    name: "file-upload",
    props: {
      maxCount: {
        required: false,
        type: Number | String,
        defaultValue: 6
      },
      label: {
        required: false,
        type: Number | String,
        defaultValue: ""
      },
      error: {
        required: false
      }
    },
    data() {
      return {
        model: [],
        images: [],
      }
    },
    model: {
      prop: "model", //绑定的值，通过父组件传递
      event: "change" //自定义时间名
    },
    methods: {
      onOversize() {
        Toast('文件大小不能超过 1000kb');
      },
      upload(file, detail) {
        file.status = 'uploading';
        file.message = '上传中...';
        return new Promise((resolve, reject) => {
        })
      },
      onDelete(file, detail) {
        const { index } = detail
        if (this.maxCount == 1) {
          this.$emit("change", null);
        }
        else {
          this.model.splice(index, 1)
          this.$emit("change", this.model);
        }
      }
    }
  }
</script>

<style>

</style>
