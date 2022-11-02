import { connect } from 'react-redux';
import { withRouter } from "react-router-dom";
import { userSessionMapStateToProps } from '../redux/action-types';
import { updateUserSession } from '../redux/actions';
import ArticleLayout from './ArticleLayout';
import UserLayoutComponent from './UserLayout';
import SearchLayout from './SearchLayout';

const UserLayout = connect(
    userSessionMapStateToProps, { updateUserSession }
)(withRouter(UserLayoutComponent))


export { UserLayout, ArticleLayout, SearchLayout };
