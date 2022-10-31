import { Button } from 'antd';
import React from 'react';

class State extends React.Component {

    likeNumber = 0

    // constructor(props: any) {
    //     super(props)
    // }

    componentWillMount() {
        console.log("componentWillMount");
    }

    // shouldComponentUpdate(nextProps: any, nextState: any): boolean {
    //     return nextState.likeNumber == this.likeNumber
    // }

    incLikeNumber(i) {
        this.setState({
            likeNumber: this.likeNumber += i
        })
    }

    render() {
        return (
            <div>
                <Button type="primary">Primary</Button>
                <Button onClick={this.incLikeNumber.bind(this, 2)} >自增2个 {this.likeNumber}</Button>
                <Button type="dashed" onClick={() => { this.incLikeNumber(1) }} >自增1个 {this.likeNumber}</Button>
                <Button type="link">Link</Button>
            </div>
        )
    }


}



export default State